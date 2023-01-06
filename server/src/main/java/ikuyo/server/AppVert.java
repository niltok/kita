package ikuyo.server;

import ikuyo.utils.AsyncVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import java.util.UUID;

public class AppVert extends AsyncVerticle {
    String nodeId;

    @Override
    public void startAsync() {
        nodeId = System.getenv("NODE_ID");
        if (nodeId == null) nodeId = UUID.randomUUID().toString();
        var config = JsonObject.of("nodeId", nodeId);
        await(vertx.deployVerticle(StarVert.class, new DeploymentOptions()
                .setWorker(true)
                .setConfig(config)
                .setInstances(100)));
    }
}
