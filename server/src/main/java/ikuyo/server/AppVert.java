package ikuyo.server;

import io.vertx.await.Async;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;

import java.util.Arrays;
import java.util.stream.Collectors;

import static io.vertx.await.Async.await;

public class AppVert extends AbstractVerticle {
    Async async;

    @Override
    public void start() throws Exception {
        async = new Async(vertx);
        async.run(v -> startAsync());
    }

    private void startAsync() {
        await(vertx.deployVerticle(SockVert.class.getName()));
        CompositeFuture.all(Arrays.stream(new int[100])
                .mapToObj(x -> vertx.deployVerticle(StarVert.class,
                        new DeploymentOptions().setWorker(true)))
                .collect(Collectors.toList()));
    }
}
