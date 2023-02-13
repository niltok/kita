package ikuyo.utils;

import io.vertx.await.Async;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.executeblocking.ExecuteBlocking;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

public interface AsyncStatic {
    static  <T> Future<T> async(Supplier<T> task) {
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

    static Future<Void> async(Runnable task) {
        return async(() -> {
            task.run();
            return null;
        });
    }

    static  <T> T await(Future<T> future, Duration timeout) {
        return Async.await(CompositeFuture.any(future, delay(timeout))).resultAt(0);
    }

    static Future<Void> delay(Duration timeout) {
        return async(() -> {
            try {
                Thread.sleep(timeout);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /** 将当前任务剩下的部分放到任务队列末尾并释放进程让其他任务先执行 */
    static void doEvents() {
        Promise<Void> promise = Promise.promise();
        Vertx.currentContext().runOnContext(v -> promise.complete());
        Async.await(promise.future());
    }

    static <T> T runBlocking(Supplier<T> task) {
        return Async.await(ExecuteBlocking.executeBlocking(task::get));
    }
}
