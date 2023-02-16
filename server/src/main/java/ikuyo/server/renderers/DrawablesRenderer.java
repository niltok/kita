package ikuyo.server.renderers;

import ikuyo.api.Drawable;
import ikuyo.api.Star;
import ikuyo.api.renderers.Renderer;
import ikuyo.server.api.RendererContext;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public interface DrawablesRenderer extends Renderer<RendererContext> {
    default JsonObject render(RendererContext ctx) {
        var drawables = new HashMap<String, Drawable>();
        renderDrawables(ctx, drawables);
        return new JsonObject(drawables.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                kv -> kv.getValue().toJson(),
                (s, a) -> s)));
    }

    void renderDrawables(RendererContext ctx, Map<String, Drawable> drawables);

    class Composite implements Renderer<RendererContext> {
        DrawablesRenderer[] renderers;
        public Composite(DrawablesRenderer... renderers) {
            this.renderers = renderers;
        }

        @Override
        public JsonObject render(RendererContext ctx) {
            var drawables = new HashMap<String, Drawable>();
            for (DrawablesRenderer renderer : renderers) {
                renderer.renderDrawables(ctx, drawables);
            }
            return new JsonObject(drawables.entrySet().stream().collect(Collectors.toMap(
                    Map.Entry::getKey,
                    kv -> kv.getValue().toJson(),
                    (s, a) -> s)));
        }
    }
}
