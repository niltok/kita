package ikuyo.server;

import io.vertx.await.Async;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.vertx.await.Async.await;

public class AppVert extends AbstractVerticle {
    Async async;
    String nodeId;

    @Override
    public void start() throws Exception {
        async = new Async(vertx);
        async.run(v -> startAsync());
    }

    private void startAsync() {
        nodeId = System.getenv("NODE_ID");
        if (nodeId == null) nodeId = UUID.randomUUID().toString();
        var config = JsonObject.of("nodeId", nodeId);
        await(vertx.deployVerticle(SockVert.class.getName()));
        await(vertx.deployVerticle(StarVert.class, new DeploymentOptions()
                .setWorker(true)
                .setConfig(config)
                .setInstances(100)));
    }
}
