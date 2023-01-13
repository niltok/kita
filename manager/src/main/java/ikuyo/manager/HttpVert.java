package ikuyo.manager;

import ikuyo.api.User;
import ikuyo.utils.AsyncVerticle;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.file.impl.FileResolverImpl;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.AllowForwardHeaders;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.FileSystemAccess;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions;
import io.vertx.ext.web.handler.sockjs.SockJSSocket;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Tuple;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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
        server = vertx.createHttpServer(new HttpServerOptions().setLogActivity(true));
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
                var user = User.getUserByToken(pool, msg.getString("token"));
                if (user == null) {
                    socket.close(4000, "auth.failed");
                    return;
                }
                registerUser(user, socket.writeHandlerID(), 3);
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
