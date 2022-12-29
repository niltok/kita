package ikuyo.server;

import ikuyo.api.Star;
import io.vertx.await.Async;
import io.vertx.core.AbstractVerticle;

public class StarVert extends AbstractVerticle {
    Async async;
    Star star;
    @Override
    public void start() throws Exception {
        super.start();
        async = new Async(vertx);
        async.run(v -> startAsync());
    }

    void startAsync() {
        var eb = vertx.eventBus();
        var starNone = eb.consumer("star-none", msg -> {
        });
        vertx.setPeriodic(1000 / 60, id -> mainLoop());
    }

    void mainLoop() {}
}
