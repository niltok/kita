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
        return AsyncStatic.async(task);
    }

    default Future<Void> async(Runnable task) {
        return AsyncStatic.async(task);
    }

    default <T> T await(Future<T> future) {
        return Async.await(future);
    }

    default <T> T await(Future<T> future, Duration timeout) {
        return AsyncStatic.await(future, timeout);
    }
}
