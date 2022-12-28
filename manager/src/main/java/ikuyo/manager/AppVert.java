package ikuyo.manager;

import io.vertx.await.Async;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;

import static io.vertx.await.Async.await;

public class AppVert extends AbstractVerticle {
    Async async;

    @Override
    public void start() throws Exception {
        super.start();
        async = new Async(vertx);
        System.out.println(Thread.currentThread().getName());
        async.run(v -> {
            System.out.println(Thread.currentThread().getName());
            await(vertx.deployVerticle(DbVert.class.getName()));
            await(vertx.deployVerticle(HttpVert.class.getName(), new DeploymentOptions()));
        });
    }
}
