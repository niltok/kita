package ikuyo.api.renderers;

import io.vertx.core.json.JsonObject;

import java.util.Arrays;

public class CompositeRenderer<T> implements Renderer<T> {
    public boolean deep;
    public Renderer<T>[] renderers;
    @SafeVarargs
    public CompositeRenderer(boolean deep, Renderer<T>... renderers) {
        this.deep = deep;
        this.renderers = renderers;
    }
    @Override
    public JsonObject render(T ctx) {
        return Arrays.stream(renderers).reduce(
                JsonObject.of(),
                (json, renderer) -> json.mergeIn(renderer.render(ctx), deep),
                (a, b) -> a.mergeIn(b, deep));
    }
}
