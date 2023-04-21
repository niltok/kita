package ikuyo.api.datatypes;

import ikuyo.utils.AsyncStatic;
import io.vertx.core.Vertx;

import java.util.stream.Stream;

public class BaseContext {
    public final Vertx vertx;
    public BaseContext(Vertx vertx) {
        this.vertx = vertx;
    }
    public <A> void parallelFor(Stream<A> stream, AsyncStatic.Consumer<A> consumer) {
        // AsyncStatic.parallelFor(vertx, stream, consumer);
        stream.map(a -> {
            var thread = AsyncStatic.threadFactory.newThread(() -> {
                try {
                    consumer.consume(a);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            });
            thread.start();
            return thread;
        }).forEach(t -> {
            while (t.isAlive()) {
                try {
                    t.join();
                } catch (InterruptedException ignored) {}
            }
        });
    }
}
