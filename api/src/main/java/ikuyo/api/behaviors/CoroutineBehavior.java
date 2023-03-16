package ikuyo.api.behaviors;

import ikuyo.utils.Enumerator;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Handler;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

import static ikuyo.utils.AsyncStatic.delay;

/** Similar to Unity3d's StartCoroutine support
 */
public class CoroutineBehavior<T> implements Behavior<T> {
    Set<Coroutine> coroutines;
    public static class Coroutine {
        Enumerator<?, ?> enumerator;
        public static class CoroutineContext {
            Enumerator<Void, Void>.Context eCtx;
            CoroutineContext(Enumerator<Void, Void>.Context eCtx) {
                this.eCtx = eCtx;
            }
            /** 等待到下一帧开始的时候再继续执行 */
            public void nextFrame() {
                eCtx.yield(null);
            }
            /** 等待 frames 帧后继续执行 */
            public void waitFrames(int frames) {
                while (frames --> 0) nextFrame();
            }
            /** Careful use it, every frame should be computed exactly most of the time */
            public void waitTime(Duration duration) {
                var fut = delay(duration);
                while (!fut.isComplete()) eCtx.yield(null);
            }
        }
        public Coroutine(Handler<CoroutineContext> task) {
            enumerator = new Enumerator<Void, Void>(ctx -> task.handle(new CoroutineContext(ctx)));
        }
    }
    /** 启动一个可跨帧运行的函数 */
    public final void startCoroutine(Handler<Coroutine.CoroutineContext> task) {
        var coroutine = new Coroutine(task);
        coroutines.add(coroutine);
        async(() -> {
            await(coroutine.enumerator.completeFuture());
            coroutines.remove(coroutine);
        });
        await(coroutine.enumerator.nextFuture());
    }

    /** do not forget call super.render(context) to render coroutines when overriding this method */
    @Override
    public void update(T context) {
        await(CompositeFuture.all(coroutines.stream().map(coroutine ->
                async(() -> {
                    coroutine.enumerator.next(null);
                    await(coroutine.enumerator.nextFuture());
                })
        ).collect(Collectors.toList())));
    }
}
