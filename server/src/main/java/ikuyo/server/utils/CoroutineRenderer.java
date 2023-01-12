package ikuyo.server.utils;

import ikuyo.utils.Enumerator;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Handler;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/** Similar to Unity3d's StartCoroutine support
 */
public class CoroutineRenderer extends AbstractRenderer {
    Set<Coroutine> coroutines;
    public class Coroutine {
        Enumerator<Void, Void> enumerator;
        public class Context {
            Enumerator<Void, Void>.Context eCtx;
            public Renderer.Context renderContext = context;
            Context(Enumerator<Void, Void>.Context eCtx) {
                this.eCtx = eCtx;
            }
            public void nextFrame() {
                eCtx.yield(null);
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
    public void init(Context context) {
        super.init(context);
        coroutines = new HashSet<>();
    }

    /** do not forget call super.render(context) to render coroutines */
    @Override
    public void render() {
        await(CompositeFuture.all(coroutines.stream().map(coroutine ->
                async(() -> {
                    coroutine.enumerator.next(null);
                    await(coroutine.enumerator.nextFuture());
                })
        ).collect(Collectors.toList())));
    }
}
