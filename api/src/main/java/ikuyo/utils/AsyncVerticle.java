package ikuyo.utils;

import io.vertx.await.Async;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;

import java.util.function.Supplier;

public abstract class AsyncVerticle extends AbstractVerticle implements AsyncHelper {
    protected Logger logger;
    protected EventBus eventBus;
    private Async asyncRunner;

    @Override
    public final void init(Vertx vertx, Context context) {
        super.init(vertx, context);
        asyncRunner = new Async(vertx);
        eventBus = vertx.eventBus();
        this.logger = LoggerFactory.getLogger("%s(%s)".formatted(this.getClass().getName(), deploymentID()));
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

    public final <T> Future<T> runBlocking(Supplier<T> task, boolean ordered) {
        return AsyncStatic.runBlocking(vertx, task, ordered);
    }

    public final <T> Future<T> runBlocking(Supplier<T> task) {
        return runBlocking(task, true);
    }
}
