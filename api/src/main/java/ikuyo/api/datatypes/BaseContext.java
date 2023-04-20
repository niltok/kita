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
        AsyncStatic.parallelFor(vertx, stream, consumer);
    }
}
