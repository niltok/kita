package ikuyo.utils;

import io.vertx.await.Async;
import io.vertx.core.*;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public abstract class AsyncVerticle implements Verticle, AsyncHelper {
    protected Vertx vertx;
    protected Context parentContext;
    protected Logger logger;
    private Async asyncRunner;

    @Override
    public final Vertx getVertx() {
        return vertx;
    }

    @Override
    public final void init(Vertx vertx, Context context) {
        this.vertx = vertx;
        asyncRunner = new Async(vertx);
        this.parentContext = context;
        this.logger = LoggerFactory.getLogger(this.getClass());
    }

    public void startAsync() throws Exception {}
    public void stopAsync() throws Exception {}

    @Override
    public final void start(Promise<Void> startPromise) {
        asyncRunner.run(v -> {
            try {
                startAsync();
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
                stopAsync();
                stopPromise.complete();
            } catch (Exception e) {
                stopPromise.fail(e);
            }
        });
    }

    public final JsonObject config() {
        return parentContext.config();
    }

}
