package ikuyo.utils;

import io.vertx.await.Async;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public abstract class AsyncVerticle implements Verticle {
    protected Vertx vertx;
    protected Context parentContext;
    private Async asyncRunner;

    @Override
    public Vertx getVertx() {
        return vertx;
    }

    @Override
    public void init(Vertx vertx, Context context) {
        this.vertx = vertx;
        asyncRunner = new Async(vertx);
        this.parentContext = context;
    }

    public void startAsync() throws Exception {}
    public void stopAsync() throws Exception {}

    @Override
    public void start(Promise<Void> startPromise) {
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
    public void stop(Promise<Void> stopPromise) {
        asyncRunner.run(v -> {
            try {
                stopAsync();
                stopPromise.complete();
            } catch (Exception e) {
                stopPromise.fail(e);
            }
        });
    }

    public <T> Future<T> async(Supplier<T> task) {
        return async(task, vertx.getOrCreateContext());
    }

    public <T> T await(Future<T> future) {
        return Async.await(future);
    }

    public JsonObject config() {
        return parentContext.config();
    }

    public static <T> Future<T> async(Supplier<T> task, @NotNull Context context) {
        Promise<T> promise = Promise.promise();
        context.runOnContext(v -> {
            try {
                promise.complete(task.get());
            } catch (Throwable e) {
                promise.fail(e);
            }
        });
        return promise.future();
    }
}
