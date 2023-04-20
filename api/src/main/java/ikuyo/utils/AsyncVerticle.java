package ikuyo.utils;

import io.vertx.await.Async;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public abstract class AsyncVerticle extends AbstractVerticle implements AsyncHelper {
    public static boolean enableMsgLog = false;
    protected Logger logger;
    protected EventBus eventBus;
    private Async asyncRunner;
    private static final AtomicBoolean inited = new AtomicBoolean();

    @Override
    public final void init(Vertx vertx, Context context) {
        super.init(vertx, context);
        asyncRunner = new Async(vertx, AsyncStatic.UseEventLoopThread);
        eventBus = vertx.eventBus();
        this.logger = LoggerFactory.getLogger("%s(%s)".formatted(this.getClass().getName(), deploymentID()));
        if (!inited.compareAndExchange(false, true)) {
            eventBus.registerDefaultCodec(NoCopyBox.class, new NoCopyBox.Codec());
        }
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

    public final <T> Future<T> runBlocking(AsyncStatic.Supplier<T> task, boolean ordered) {
        return AsyncStatic.runBlocking(vertx, task, ordered);
    }

    public final Future<Void> runBlocking(AsyncStatic.Task task, boolean ordered) {
        return runBlocking(() -> {
            task.run();
            return null;
        }, ordered);
    }

    public final <A, B> List<B> parallelMap(Stream<A> stream, AsyncStatic.Mapper<A, B> mapper) {
        return AsyncStatic.parallelMap(vertx, stream, mapper);
    }

    public final <A> void parallelFor(Stream<A> stream, AsyncStatic.Consumer<A> consumer) {
        AsyncStatic.parallelFor(vertx, stream, consumer);
    }

    public final <T> Future<T> runBlocking(AsyncStatic.Supplier<T> task) {
        return runBlocking(task, true);
    }

    public final Future<Void> runBlocking(AsyncStatic.Task task) {
        return runBlocking(task, true);
    }
}
