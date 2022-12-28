package ikuyo.server;

import io.vertx.await.Async;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;

import static io.vertx.await.Async.await;

public class SockVert extends AbstractVerticle {
    Async async;
    Router router;
    HttpServer server;

    @Override
    public void start() throws Exception {
        async = new Async(vertx);
        async.run(v -> {
            router = Router.router(vertx);
            var sockjsHandler = SockJSHandler.create(vertx);
            router.route("/*").subRouter(sockjsHandler.socketHandler(socket -> {
                socket.handler(buffer -> {
                    await(socket.write(buffer));
                });
            }));
            server = vertx.createHttpServer();
            await(server.requestHandler(router).listen(8071));
        });
    }
}
