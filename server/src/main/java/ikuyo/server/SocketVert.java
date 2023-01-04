package ikuyo.server;

import ikuyo.api.User;
import io.vertx.await.Async;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.handler.sockjs.SockJSSocket;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;

import static io.vertx.await.Async.await;

public class SocketVert extends AbstractVerticle {
    Async async;
    Router router;
    HttpServer server;
    PgPool pool;

    @Override
    public void start() throws Exception {
        async = new Async(vertx);
        async.run(v -> startAsync());
    }

    private void startAsync() {
        pool = PgPool.pool(vertx, new PoolOptions());
        router = Router.router(vertx);
        var sockjsHandler = SockJSHandler.create(vertx);
        router.route("/*").subRouter(sockjsHandler.socketHandler(socket -> {
            socket.handler(buffer -> {
                var msg = new JsonObject(buffer);
                socketHandler(socket, msg);
            });
        }));
        server = vertx.createHttpServer();
        await(server.requestHandler(router).listen(8071));
        System.out.println("listening...");
    }

    private void socketHandler(SockJSSocket socket, JsonObject msg) {
        switch (msg.getString("type")) {
            case "auth.request" -> {
                var user = User.getUserByToken(pool, msg.getString("token"));
                if (user == null) {
                    socket.close(4000, "auth.failed");
                    return;
                }
                socket.write(JsonObject.of("type", "auth.pass").toBuffer());
            }
        }
    }
}
