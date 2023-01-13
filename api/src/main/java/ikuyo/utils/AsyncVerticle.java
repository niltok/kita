package ikuyo.utils;

import io.vertx.await.Async;
import io.vertx.core.*;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;

public abstract class AsyncVerticle extends AbstractVerticle implements AsyncHelper {
    protected Logger logger;
    private Async asyncRunner;

    @Override
    public final void init(Vertx vertx, Context context) {
        super.init(vertx, context);
        asyncRunner = new Async(vertx, true);
        this.logger = LoggerFactory.getLogger(this.getClass());
    }

    @Override
    public final void start(Promise<Void> startPromise) {
        asyncRunner.run(v -> {
            try {
                start();
                startPromise.complete();
            } catch (Exception e) {
                startPromise.fail(e);
            }
        });
    }

    @Override
    public final void stop(Promise<Void> stopPromise) {
        asyncRunner.run(v -> {
            try {
                stop();
                stopPromise.complete();
            } catch (Exception e) {
                stopPromise.fail(e);
            }
        });
    }
}
