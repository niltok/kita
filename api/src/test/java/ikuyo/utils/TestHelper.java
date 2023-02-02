package ikuyo.utils;

import io.vertx.core.Vertx;

public interface TestHelper {
    default void runAsync(Runnable task) {
        var vertx = Vertx.vertx();
        vertx.deployVerticle(new AsyncVerticle() {
            @Override
            public void start() throws Exception {
                task.run();
            }
        });
    }
}
