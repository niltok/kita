package ikuyo.server.renderers;

import ikuyo.api.renderers.CompositeRenderer;
import ikuyo.api.renderers.Renderer;
import ikuyo.server.api.CommonContext;
import ikuyo.utils.AsyncStatic;
import io.vertx.await.Async;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;

public class ParallelRenderer extends CompositeRenderer<CommonContext> {
    @SafeVarargs
    public ParallelRenderer(boolean deep, Renderer<CommonContext>... renderers) {
        super(deep, renderers);
    }
    @Override
    public JsonObject render(CommonContext ctx) {
        return Arrays.stream(renderers)
                .map(renderer -> AsyncStatic.runBlocking(ctx.vertx, () -> renderer.render(ctx), false))
                .reduce(
                    JsonObject.of(),
                    (json, future) -> json.mergeIn(Async.await(future), deep),
                    (a, b) -> a.mergeIn(b, deep));
    }
}
