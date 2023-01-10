package ikuyo;

import ikuyo.utils.AsyncVerticle;

public class BundleVert extends AsyncVerticle {
    @Override
    public void startAsync() {
        await(vertx.deployVerticle(ikuyo.server.AppVert.class.getName()));
        await(vertx.deployVerticle(ikuyo.manager.AppVert.class.getName()));
    }
}