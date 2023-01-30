package ikuyo.manager;

import ikuyo.api.User;
import ikuyo.utils.AsyncVerticle;
import io.netty.handler.codec.compression.CompressionOptions;
import io.netty.handler.codec.compression.GzipOptions;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.AllowForwardHeaders;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions;
import io.vertx.ext.web.handler.sockjs.SockJSSocket;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Tuple;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HttpVert extends AsyncVerticle {
    HttpServer server;
    PgPool pool;
    EventBus eb;
    Router router;
    Map<String, User> socketCache = new HashMap<>();

    @Override
    public void start() {
        eb = vertx.eventBus();
        pool = PgPool.pool(vertx, new PoolOptions());
        server = vertx.createHttpServer(new HttpServerOptions()
                .setLogActivity(true).setCompressionSupported(true));
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
            eb.send(socketAddress(socket), JsonObject.of(
                    "type", "user.disconnect",
                    "id", socketCache.get(socket.writeHandlerID()).id()));
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

    void registerUser(User user, String socket, int retry) {
        try {
            var loaded = await(pool.preparedQuery("""
                select * from star where index = $1 and vert_id is not null
                """).execute(Tuple.of(user.star()))).rowCount() == 1;
            if (!loaded) {
                await(eb.request("star.none", JsonObject.of(
                        "type", "star.load", "id", user.star()
                ), new DeliveryOptions().setSendTimeout(5000)));
            }
            await(eb.request("star." + user.star(), JsonObject.of(
                    "type", "user.add", "socket", socket, "id", user.id()
            ), new DeliveryOptions().setSendTimeout(1000)));
        } catch (Exception e) {
            if (retry > 0) registerUser(user, socket, retry - 1);
            else throw new RuntimeException("server busy");
        }
    }

    private void socketHandler(SockJSSocket socket, @NotNull JsonObject msg) {
        switch (msg.getString("type")) {
            case "auth.request" -> {
                var user = User.getByToken(pool, msg.getString("token"));
                if (user == null) {
                    socket.close(4000, "auth.failed");
                    return;
                }
                registerUser(user, socket.writeHandlerID(), 3);
                socketCache.put(socket.writeHandlerID(), user);
                await(socket.write(JsonObject.of("type", "auth.pass").toBuffer()));
            }
            case "state.seq.require" -> {
                switch (msg.getString("target")) {
                    case "starDrawables" ->
                            eb.send(socketAddress(socket), JsonObject.of(
                                    "type", msg.getString("type"),
                                    "socket", socket.writeHandlerID(),
                                    "userId", socketCache.get(socket.writeHandlerID()).id(),
                                    "msg", msg));
                    default -> {}
                }
            }
            default -> {
                eb.send(socketAddress(socket), JsonObject.of(
                        "type", "user.message",
                        "socket", socket.writeHandlerID(),
                        "userId", socketCache.get(socket.writeHandlerID()).id(),
                        "msg", msg));
            }
        }
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
