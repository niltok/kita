package ikuyo.utils;

import io.vertx.await.Async;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.net.impl.pool.Task;

import java.time.Duration;
import java.util.concurrent.Callable;
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

    /** 将当前任务剩下的部分放到任务队列末尾并释放进程让其他任务先执行 */
    default void doEvents() { AsyncStatic.doEvents(); }

    default <T> T runBlocking(Supplier<T> task) { return AsyncStatic.runBlocking(task); }

    default void runBlocking(Runnable task) { AsyncStatic.runBlocking(() -> task); }
}
