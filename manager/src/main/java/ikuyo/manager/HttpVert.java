package ikuyo.manager;

import ikuyo.api.Star;
import ikuyo.api.User;
import ikuyo.api.behaviors.Behavior;
import ikuyo.api.behaviors.CompositeBehavior;
import ikuyo.api.renderers.CompositeRenderer;
import ikuyo.api.renderers.Renderer;
import ikuyo.manager.api.*;
import ikuyo.manager.behaviors.StarMapBehavior;
import ikuyo.manager.renderers.StarMapRenderer;
import ikuyo.manager.renderers.UIRenderer;
import ikuyo.utils.AsyncVerticle;
import io.reactivex.rxjava3.subjects.Subject;
import io.reactivex.rxjava3.subjects.UnicastSubject;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.*;
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

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static ikuyo.utils.AsyncStatic.delay;
import static ikuyo.utils.MsgDiffer.jsonDiff;

public class HttpVert extends AsyncVerticle {
    HttpServer server;
    PgPool pool;
    Router router;
    Map<String, User> socketCache = new HashMap<>();
    Subject<Integer> render$ = UnicastSubject.create();
    CommonContext commonContext;
    UpdatedContext updatedContext;
    BehaviorContext behaviorContext;
    RendererContext rendererContext;
    Renderer<RendererContext> uiRenderer = new UIRenderer.Composite(
            new StarMapRenderer()
    );
    Behavior<BehaviorArgContext> mainBehavior = new CompositeBehavior<>(
            new StarMapBehavior()
    );

    @Override
    public void start() {
        pool = PgPool.pool(vertx, new PoolOptions());
        server = vertx.createHttpServer(new HttpServerOptions()
                .setLogActivity(true).setCompressionSupported(true));
        updatedContext = new UpdatedContext();
        commonContext = new CommonContext(pool, updatedContext);
        rendererContext = new RendererContext(commonContext);
        behaviorContext = new BehaviorContext(render$, commonContext);
        render$.subscribe(i -> renderUI());
        router = Router.router(vertx);
        router.allowForward(AllowForwardHeaders.ALL);
        // sockjs handler ????????????????????? async handler ????????????????????????
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
        router.route().handler(StaticHandler.create());
        server.requestHandler(router);
        await(server.listen(8070));
        System.out.println("listening...");
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
            if (socketCache.get(socket.writeHandlerID()) == null) return;
            eventBus.send(socketAddress(socket), JsonObject.of(
                    "type", "user.disconnect",
                    "id", socketCache.get(socket.writeHandlerID()).id()));
            commonContext.userState().remove(socketCache.get(socket.writeHandlerID()).id());
            socketCache.remove(socket.writeHandlerID());
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

    static final int timeout = 10000;
    void registerUser(User user, String socket, int retry) {
        try {
            var summery = Star.getSummery(pool, user.star());
            assert summery != null;
            async(() -> Star.query(pool, user.universe(),
                    summery.x() - Star.viewRange, summery.x() + Star.viewRange,
                    summery.y() - Star.viewRange, summery.y() + Star.viewRange));
            if (summery.vertId() == null) {
                await(eventBus.request("star.none", JsonObject.of(
                        "type", "star.load", "id", user.star()
                ), new DeliveryOptions().setSendTimeout(timeout)));
            }
            await(eventBus.request("star." + user.star(), JsonObject.of(
                    "type", "user.add", "socket", socket, "id", user.id()
            ), new DeliveryOptions().setSendTimeout(timeout)));
        } catch (Exception e) {
            // ???????????????????????????
            if (retry < 3) {
                await(pool.preparedQuery("""
                        update star set vert_id = null where index = $1
                        """).execute(Tuple.of(user.star())));
                logger.info(JsonObject.of("type", "star.ownership.release", "starId", user.star()));
            }
            if (retry > 0) {
                await(delay(Duration.ofSeconds(1)));
                logger.info(JsonObject.of("type", "user.register.retry", "remain", retry));
                registerUser(user, socket, retry - 1);
            }
            else throw new RuntimeException("server busy");
        }
    }

    private void socketHandler(SockJSSocket socket, @NotNull JsonObject msg) {
        logger.info(msg);
        switch (msg.getString("type")) {
            case "auth.request" -> {
                var user = User.getByToken(pool, msg.getString("token"));
                if (user == null) {
                    socket.close(4000, "auth.failed");
                    return;
                }
                registerUser(user, socket.writeHandlerID(), 5);
                socketCache.put(socket.writeHandlerID(), user);
                commonContext.userState().put(user.id(), new UserState(socket.writeHandlerID(), user));
                mainBehavior.update(new BehaviorArgContext(user.id(), msg, behaviorContext));
                renderUI();
                await(socket.write(JsonObject.of("type", "auth.pass").toBuffer()));
            }
            case "user.move" -> {
                var target = msg.getInteger("target");
                var id = socketCache.get(socket.writeHandlerID()).id();
                await(eventBus.request(socketAddress(socket), JsonObject.of(
                        "type", "user.remove", "id", id)));
                await(pool.preparedQuery("""
                    update "user" set star = $2 where id = $1
                    """).execute(Tuple.of(id, target)));
                var user = User.getUserById(pool, id);
                assert user != null;
                registerUser(user, socket.writeHandlerID(), 3);
                socketCache.put(socket.writeHandlerID(), user);
                commonContext.userState().get(id).user = user;
                commonContext.updated().users().add(id);
                renderUI();
                await(socket.write(JsonObject.of("type", "move.success").toBuffer()));
            }
            default -> {
                var id = socketCache.get(socket.writeHandlerID()).id();
                mainBehavior.update(new BehaviorArgContext(id, msg, behaviorContext));
                renderUI();
                eventBus.send(socketAddress(socket), JsonObject.of(
                        "type", "user.message",
                        "socket", socket.writeHandlerID(),
                        "userId", id,
                        "msg", msg));
            }
        }
    }

    private void renderUI() {
        uiRenderer.render(rendererContext).forEach(e -> {
            var id = Integer.valueOf(e.getKey());
            var json = (JsonObject) e.getValue();
            var state = commonContext.userState().get(id);
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
        updatedContext.clear();
    }

    @NotNull
    private String socketAddress(SockJSSocket socket) {
        return "star." + socketCache.get(socket.writeHandlerID()).star();
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
