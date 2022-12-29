package ikuyo.manager;

import io.vertx.await.Async;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;

import static io.vertx.await.Async.await;

public class HttpVert extends AbstractVerticle {
    Async async;
    HttpServer server;
    @Override
    public void start() throws Exception {
        super.start();
        async = new Async(vertx);
        server = vertx.createHttpServer();
        System.out.println(Thread.currentThread().getName());
        server.requestHandler(req -> {
            System.out.println(Thread.currentThread().getName());
            //language=HTML
            req.response().end("""
            <!DOCTYPE html>
            <html lang="zh-cn">
            <body>
            <h1>hello</h1>
            </body>
            </html>
            """);
        });
        async.run(v -> {
            System.out.println(Thread.currentThread().getName());
            var ctx = vertx.getOrCreateContext();
            ctx.runOnContext(vv -> {
                System.out.println(Thread.currentThread().getName());
            });
            await(server.listen(8070));
            System.out.println("listening...");
        });
    }
}
