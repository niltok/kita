package ikuyo.utils;

import io.vertx.await.Async;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

public interface AsyncStatic {
    boolean UseEventLoopThread = false;

    @FunctionalInterface
    interface Supplier<T> {
        T get() throws Throwable;
    }

    @FunctionalInterface
    interface Consumer<T> {
        void consume(T value) throws Throwable;
    }

    @FunctionalInterface
    interface Mapper<A, B> {
        B map(A value) throws Throwable;
    }

    @FunctionalInterface
    interface Task {
        void run() throws Throwable;
    }

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
        var startTime = Instant.now();
        return async(() -> {
            while (Instant.now().isBefore(startTime.plus(timeout))) doEvents();
        });
    }

    /** 将当前任务剩下的部分放到任务队列末尾并释放进程让其他任务先执行 */
    static void doEvents() {
        Promise<Void> promise = Promise.promise();
        Vertx.currentContext().runOnContext(v -> promise.complete());
        Async.await(promise.future());
    }

    static <T> Future<T> runBlocking(Vertx vertx, Supplier<T> task, boolean ordered) {
        return vertx.executeBlocking(promise -> new Async(vertx, UseEventLoopThread)
                .run(v -> {
                    try {
                        promise.complete(task.get());
                    } catch (Throwable e) {
                        promise.fail(e);
                    }
                }), ordered);
    }

    static <A, B> List<B> parallelMap(Vertx vertx, Stream<A> stream, Mapper<A, B> f) {
        return stream.map(a -> runBlocking(vertx, () -> f.map(a), false)).map(Async::await).toList();
    }

    static  <A> void parallelFor(Vertx vertx, Stream<A> stream, AsyncStatic.Consumer<A> consumer) {
        parallelMap(vertx, stream, a -> {
            consumer.consume(a);
            return null;
        });
    }
    static <T> Future<T> runBlocking(Vertx vertx, Supplier<T> task) {
        return runBlocking(vertx, task, true);
    }
}
