package ikuyo.server;

import io.vertx.await.Async;
import io.vertx.core.AbstractVerticle;

import static io.vertx.await.Async.await;

public class AppVert extends AbstractVerticle {
    Async async;

    @Override
    public void start() throws Exception {
        async = new Async(vertx);
        async.run(v -> {
            await(vertx.deployVerticle(SockVert.class.getName()));
        });
    }
}
