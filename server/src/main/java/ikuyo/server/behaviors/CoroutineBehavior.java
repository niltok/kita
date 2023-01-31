package ikuyo.server.behaviors;

import ikuyo.server.api.Behavior;
import ikuyo.utils.Enumerator;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Handler;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static ikuyo.utils.AsyncStatic.delay;

/** Similar to Unity3d's StartCoroutine support
 */
public class CoroutineBehavior extends AbstractBehavior {
    Set<Coroutine> coroutines;
    public class Coroutine {
        Enumerator<Void, Void> enumerator;
        public class Context {
            Enumerator<Void, Void>.Context eCtx;
            public Behavior.Context renderContext = context;
            Context(Enumerator<Void, Void>.Context eCtx) {
                this.eCtx = eCtx;
            }
            public void nextFrame() {
                eCtx.yield(null);
            }
            /** Careful use it, every frame should be computed exactly most of the time */
            public void waitTime(Duration duration) {
                var fut = delay(duration);
                while (!fut.isComplete()) eCtx.yield(null);
            }
        }
        public Coroutine(Handler<Context> task) {
            //noinspection unchecked
            enumerator = new Enumerator<>(ctx -> task.handle(new Context(ctx)));
        }
    }
    public final void startCoroutine(Handler<Coroutine.Context> task) {
        var coroutine = new Coroutine(task);
        coroutines.add(coroutine);
        async(() -> {
            await(coroutine.enumerator.completeFuture());
            coroutines.remove(coroutine);
        });
        await(coroutine.enumerator.nextFuture());
    }

    @Override
    public void setContext(Context context) {
        super.setContext(context);
        coroutines = new HashSet<>();
    }

    /** do not forget call super.render(context) to render coroutines when overriding this method */
    @Override
    public void update() {
        await(CompositeFuture.all(coroutines.stream().map(coroutine ->
                async(() -> {
                    coroutine.enumerator.next(null);
                    await(coroutine.enumerator.nextFuture());
                })
        ).collect(Collectors.toList())));
    }
}
