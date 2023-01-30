package ikuyo.server.renderers;

import ikuyo.api.Star;
import ikuyo.server.api.Renderer;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;

public class CompositeRenderer implements Renderer {
    Renderer[] renderers;
    public CompositeRenderer(Renderer... renderers) {
        this.renderers = renderers;
    }
    @Override
    public JsonObject render(Star star) {
        return Arrays.stream(renderers).reduce(
                JsonObject.of(),
                (json, renderer) -> renderer.render(star),
                (a, b) -> a.mergeIn(b, true));
    }
}
