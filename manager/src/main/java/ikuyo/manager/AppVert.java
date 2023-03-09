package ikuyo.manager;

import ikuyo.utils.AsyncVerticle;
import io.vertx.core.DeploymentOptions;

public class AppVert extends AsyncVerticle {
    @Override
    public void start() throws Exception {
        await(vertx.deployVerticle(DbVert.class.getName()));
        await(vertx.deployVerticle(HttpVert.class, new DeploymentOptions().setInstances(10)));
    }
}
