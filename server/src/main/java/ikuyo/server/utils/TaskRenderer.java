package ikuyo.server.utils;

import ikuyo.utils.Enumerator;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Handler;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Renderer with async task support
 */
public class TaskRenderer extends AbstractRenderer {
    Set<AsyncTask> asyncTasks;
    public class AsyncTask {
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
        public AsyncTask(Handler<Context> task) {
            //noinspection unchecked
            enumerator = new Enumerator<>(ctx -> task.handle(new Context(ctx)));
        }
    }
    public final void addTask(Handler<AsyncTask.Context> task) {
        var asyncTask = new AsyncTask(task);
        asyncTasks.add(asyncTask);
        async(() -> {
            await(asyncTask.enumerator.completeFuture());
            asyncTasks.remove(asyncTask);
        });
        await(asyncTask.enumerator.nextFuture());
    }

    @Override
    public void init(Context context) {
        super.init(context);
        asyncTasks = new HashSet<>();
    }

    /**
     * do not forget call super.render(context)
     */
    @Override
    public void render() {
        await(CompositeFuture.all(asyncTasks.stream().map(task ->
                async(() -> {
                    task.enumerator.next(null);
                    await(task.enumerator.nextFuture());
                })
        ).collect(Collectors.toList())));
    }
}
