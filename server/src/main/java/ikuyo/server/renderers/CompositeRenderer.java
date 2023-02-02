package ikuyo.server.renderers;

import ikuyo.api.Star;
import ikuyo.server.api.Renderer;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;

public class CompositeRenderer implements Renderer {
    boolean deep;
    Renderer[] renderers;
    public CompositeRenderer(boolean deep, Renderer... renderers) {
        this.deep = deep;
        this.renderers = renderers;
    }
    @Override
    public JsonObject render(Star star) {
        return Arrays.stream(renderers).reduce(
                JsonObject.of(),
                (json, renderer) -> json.mergeIn(renderer.render(star), deep),
                (a, b) -> a.mergeIn(b, deep));
    }
}
