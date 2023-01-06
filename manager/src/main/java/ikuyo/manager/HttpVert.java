package ikuyo.manager;

import ikuyo.api.User;
import ikuyo.utils.AsyncVerticle;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServer;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
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
    HttpClient client;
    PgPool pool;
    EventBus eb;
    Router router;
    Map<String, User> socketCache = new HashMap<>();

    @Override
    public void startAsync() {
        eb = vertx.eventBus();
        pool = PgPool.pool(vertx, new PoolOptions());
        server = vertx.createHttpServer();
        client = vertx.createHttpClient();
        router = Router.router(vertx);
        router.allowForward(AllowForwardHeaders.ALL);
        // sockjs handler 前面不能加任何 async handler 所以别改这段代码
        // == do not edit start ==
        router.route()
                .handler(CorsHandler.create())
                .handler(BodyHandler.create());
        router.route("/socket/*").subRouter(
                SockJSHandler.create(vertx,
                        new SockJSHandlerOptions()
                                .setRegisterWriteHandler(true)
                                .setLocalWriteHandler(false))
                        .socketHandler(this::handleSocket));
        // == do not edit end ==
        router.get("/").handler(req -> req.reroute("/index.html"));
        router.post("/login").handler(this::loginHandler);
        router.route().handler(StaticHandler.create(System.getenv("STATIC_PATH")));
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
            eb.send(socketAddress(socket), JsonObject.of(
                    "type", "user.disconnect",
                    "id", socketCache.get(socket.writeHandlerID()).id()));
            socketCache.remove(socket.writeHandlerID());
        });
    }

    void loginHandler(RoutingContext req) {
        var name = req.request().getHeader("name");
        var pwd = req.request().getHeader("pwd");
        var user = User.getUserByName(pool, name);
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

    void registerUser(User user, String socket) {
        try {
            await(eb.request("star." + user.star(), JsonObject.of(
                    "type", "user.add", "socket", socket, "id", user.id()
            ), new DeliveryOptions().setSendTimeout(1000)));
        } catch (Exception e) {
            try {
                await(eb.request("star.none", JsonObject.of(
                        "type", "star.load", "id", user.star()
                ), new DeliveryOptions().setSendTimeout(5000)));
                await(eb.request("star." + user.star(), JsonObject.of(
                        "type", "user.add", "socket", socket, "id", user.id()
                ), new DeliveryOptions().setSendTimeout(1000)));
            } catch (Exception ignored) {
                throw new RuntimeException("server busy");
            }
        }
    }

    private void socketHandler(SockJSSocket socket, @NotNull JsonObject msg) {
        switch (msg.getString("type")) {
            case "auth.request" -> {
                var user = User.getUserByToken(pool, msg.getString("token"));
                if (user == null) {
                    socket.close(4000, "auth.failed");
                    return;
                }
                registerUser(user, socket.writeHandlerID());
                socketCache.put(socket.writeHandlerID(), user);
                await(socket.write(JsonObject.of("type", "auth.pass").toBuffer()));
            }
            case "user.operate.map" ->
                    eb.send(socketAddress(socket), msg);
        }
    }

    @NotNull
    private String socketAddress(SockJSSocket socket) {
        return "star." + socketCache.get(socket.writeHandlerID()).star();
    }
}
