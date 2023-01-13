package ikuyo.manager;

import ikuyo.utils.AsyncVerticle;

public class AppVert extends AsyncVerticle {
    @Override
    public void start() throws Exception {
        await(vertx.deployVerticle(DbVert.class.getName()));
        await(vertx.deployVerticle(HttpVert.class.getName()));
    }
}
