package ikuyo.utils;

import io.vertx.await.Async;
import io.vertx.core.*;

public class Enumerator<T, U> implements AsyncHelper {
    Promise<Void> complete;
    Promise<T> nextValue;
    Promise<U> suspend;
    public class Context {
        public U yield(T value) {
            nextValue.complete(value);
            var val = await(suspend.future());
            suspend = Promise.promise();
            return val;
        }
    }
    public Enumerator(Handler<Context> generator) {
        complete = Promise.promise();
        suspend = Promise.promise();
        nextValue = Promise.promise();
        async(() -> {
            generator.handle(new Context());
            complete.complete();
        });
    }
    public T next(U value) {
        if (!hasNext()) return null;
        var val = await(nextValue.future());
        nextValue = Promise.promise();
        suspend.complete(value);
        return val;
    }
    public T tryNext(U value) {
        if (nextValue.future().isComplete()) return next(value);
        return null;
    }

    /**
     * will not continue on complete
     * <p>
     * call next() after complete
     */
    public Future<T> nextFuture() {
        return nextValue.future();
    }
    public Future<Void> completeFuture() {
        return complete.future();
    }
    public boolean hasNext() {
        await(CompositeFuture.any(nextValue.future(), complete.future()));
        return !complete.future().isComplete();
    }
}
