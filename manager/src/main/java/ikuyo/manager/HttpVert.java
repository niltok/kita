package ikuyo.manager;

import ikuyo.api.behaviors.Behavior;
import ikuyo.api.behaviors.CompositeBehavior;
import ikuyo.api.entities.User;
import ikuyo.api.renderers.CompositeRenderer;
import ikuyo.api.renderers.Renderer;
import ikuyo.api.renderers.UIRenderer;
import ikuyo.manager.api.CommonContext;
import ikuyo.manager.behaviors.*;
import ikuyo.manager.renderers.StarMapRenderer;
import ikuyo.manager.renderers.TechTrainerRenderer;
import ikuyo.manager.renderers.TransferRenderer;
import ikuyo.utils.AsyncVerticle;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.AllowForwardHeaders;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions;
import io.vertx.ext.web.handler.sockjs.SockJSSocket;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Tuple;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.UUID;

import static ikuyo.utils.MsgDiffer.jsonDiff;

public class HttpVert extends AsyncVerticle {
    static long millisPerFrame = 500;
    HttpServer server;
    PgPool pool;
    Router router;
    CommonContext commonContext;
    long mainLoopId;
    Behavior<CommonContext> mainBehavior = new CompositeBehavior<>(
            new PageBehavior(),
            new TransferBehavior(),
            new CargoBehavior(),
            new StarMapBehavior(),
            new TechTrainerBehavior(),
            new UserManageBehavior()
    );
    Renderer<CommonContext> uiRenderer = new CompositeRenderer<>(true,
            new UIRenderer.Composite<>(
                    new StarMapRenderer(),
                    new TechTrainerRenderer(),
                    new TransferRenderer()
            ).withName("ui")
    );

    @Override
    public void start() {
        pool = PgPool.pool(vertx, new PoolOptions());
        server = vertx.createHttpServer(new HttpServerOptions()
                .setLogActivity(true).setCompressionSupported(true));
        commonContext = new CommonContext(vertx, pool, eventBus, logger);
        router = Router.router(vertx);
        router.allowForward(AllowForwardHeaders.ALL);
        // sockjs handler 前面不能加任何 async handler 所以别改这段代码
        // == do not edit start ==
        router.route()
                .handler(BodyHandler.create())
                .handler(HttpVert::CorsHandler);
        router.route("/socket/*").subRouter(
                SockJSHandler.create(vertx,
                        new SockJSHandlerOptions()
                                .setRegisterWriteHandler(true)
                                .setLocalWriteHandler(false))
                        .socketHandler(this::handleSocket));
        // == do not edit end ==
        router.post("/login").handler(this::loginHandler);
        router.route().handler(StaticHandler.create().setCachingEnabled(false));
        server.requestHandler(router);
        await(server.listen(8070));
        mainLoopId = vertx.setPeriodic(millisPerFrame, l -> mainLoop());
        System.out.println("listening...");
    }

    @Override
    public void stop() throws Exception {
        vertx.cancelTimer(mainLoopId);
    }

    private void handleSocket(SockJSSocket socket) {
        socket.handler(buffer -> {
            var msg = new JsonObject(buffer);
            try {
                socketHandler(socket, msg);
            } catch (Exception e) {
                socket.close(1013, e.getLocalizedMessage());
                e.printStackTrace();
            }
        });
        socket.closeHandler(v -> {
            var user = commonContext.getUser(socket.writeHandlerID());
            if (user == null) return;
            eventBus.send(socketAddress(socket), JsonObject.of(
                    "type", "user.disconnect",
                    "id", user.id()));
            commonContext.userState().remove(user.id());
            commonContext.socketCache().remove(socket.writeHandlerID());
            commonContext.updated().users().add(user.id());
        });
    }

    void loginHandler(RoutingContext req) {
        var name = req.request().getHeader("name");
        var pwd = req.request().getHeader("pwd");
        var user = User.getByName(pool, name);
        if (user == null || !user.pwd().equals(pwd)) {
            await(req.response().setStatusCode(401).end("no such a user or password wrong"));
            return;
        }
        var token = UUID.randomUUID().toString();
        await(pool.preparedQuery("""
            update "user" set token = $2 where name = $1
            """).execute(Tuple.of(name, token)));
        await(req.response().end(token));
    }

    private void socketHandler(SockJSSocket socket, @NotNull JsonObject msg) {
        if (enableMsgLog) logger.info(msg);
        switch (msg.getString("type")) {
            case "auth.request" -> {
                var user = User.getByToken(pool, msg.getString("token"));
                if (user == null) {
                    socket.close(4000, "auth.failed");
                    return;
                }
                if (commonContext.userState().get(user.id()) != null) {
                    socket.close(4001, "auth.repeat");
                    return;
                }
                commonContext.addUser(socket.writeHandlerID(), user);
                commonContext.registerUser(user, socket.writeHandlerID(), null, 6);
                await(socket.write(JsonObject.of("type", "auth.pass").toBuffer()));
            }
            case "user.move.star" -> {
                var target = msg.getInteger("target");
                var id = commonContext.getUser(socket.writeHandlerID()).id();
                commonContext.getState(id).page = "transfer";
                commonContext.updated().users().add(id);
                var res = (JsonObject) await(eventBus.request(socketAddress(socket), JsonObject.of(
                        "type", "user.remove", "id", id))).body();
                await(pool.preparedQuery("""
                    update "user" set star = $2 where id = $1
                    """).execute(Tuple.of(id, target)));
                var user = User.getUserById(pool, id);
                assert user != null;
                commonContext.registerUser(user, socket.writeHandlerID(),
                        res.getJsonObject("userInfo"), 3);
                commonContext.addUser(socket.writeHandlerID(), user);
            }
            default -> {
                var id = commonContext.getUser(socket.writeHandlerID()).id();
                var state = commonContext.userState().get(id);
                state.events.computeIfAbsent(msg.getString("type"), i -> new ArrayList<>()).add(msg);
                commonContext.updated().users().add(id);
                if (state.allowOperate()) {
                    eventBus.send(socketAddress(socket), JsonObject.of(
                            "type", "user.message",
                            "socket", socket.writeHandlerID(),
                            "userId", id,
                            "msg", msg));
                }
            }
        }
    }

    private void mainLoop() {
        mainBehavior.update(commonContext);
        try {
            uiRenderer.render(commonContext).forEach(e -> {
                var id = Integer.parseInt(e.getKey());
                var json = (JsonObject) e.getValue();
                var state = commonContext.getState(id);
                if (state == null) return;
                var diff = jsonDiff(state.cache, json);
                if (!diff.isEmpty()) {
                    state.cache = json;
                    eventBus.send(state.socket, JsonObject.of(
                            "type", "state.dispatch",
                            "action", "gameState/diffGame",
                            "payload", diff
                    ).toBuffer());
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
        }
        commonContext.frame();
    }

    @NotNull
    private String socketAddress(SockJSSocket socket) {
        return "star." + commonContext.getUser(socket.writeHandlerID()).star();
    }

    public static void CorsHandler(RoutingContext ctx) {
        var request = ctx.request();
        var origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin == null) origin = "*";
        var response = ctx.response();
        response.putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
        response.putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "*");
        response.putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        response.putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "*");
        if (request.method() == HttpMethod.OPTIONS) response.setStatusCode(204).end();
        else ctx.next();
    }
}
