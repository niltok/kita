package ikuyo.utils;

import io.vertx.await.Async;
import io.vertx.core.Future;

import java.time.Duration;
import java.util.concurrent.locks.Lock;

public interface AsyncHelper {
    default <T> Future<T> async(AsyncStatic.Supplier<T> task) {
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

    default void lock(Lock lock) {
        Async.lock(lock);
    }

    /** 将当前任务剩下的部分放到任务队列末尾并释放进程让其他任务先执行 */
    default void doEvents() { AsyncStatic.doEvents(); }
}
