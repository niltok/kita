package ikuyo.utils;

import io.vertx.await.Async;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

import java.time.Duration;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

public interface AsyncHelper {
    default <T> Future<T> async(Supplier<T> task) {
        Promise<T> promise = Promise.promise();
        Vertx.currentContext().runOnContext(v -> {
            try {
                promise.complete(task.get());
            } catch (Throwable e) {
                promise.fail(e);
            }
        });
        return promise.future();
    }

    default Future<Void> async(Runnable task) {
        return async(() -> {
            task.run();
            return null;
        });
    }

    default <T> T await(Future<T> future) {
        return Async.await(future);
    }

    default <T> T await(Future<T> future, Duration timeout) {
        return await(CompositeFuture.any(future, delay(timeout))).resultAt(0);
    }

    default Future<Void> delay(Duration timeout) {
        return async(() -> {
            try {
                Thread.sleep(timeout);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
