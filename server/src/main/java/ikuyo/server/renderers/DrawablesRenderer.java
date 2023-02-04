package ikuyo.server.renderers;

import ikuyo.api.Drawable;
import ikuyo.api.Star;
import ikuyo.server.api.Renderer;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public interface DrawablesRenderer extends Renderer {
    default JsonObject render(Context ctx) {
        var drawables = new HashMap<String, Drawable>();
        renderDrawables(ctx.star(), drawables);
        return new JsonObject(drawables.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                kv -> JsonObject.mapFrom(kv.getValue()),
                (s, a) -> s)));
    }

    void renderDrawables(Star star, Map<String, Drawable> drawables);

    class Composite implements Renderer {
        DrawablesRenderer[] renderers;
        public Composite(DrawablesRenderer... renderers) {
            this.renderers = renderers;
        }

        @Override
        public JsonObject render(Context ctx) {
            var drawables = new HashMap<String, Drawable>();
            for (DrawablesRenderer renderer : renderers) {
                renderer.renderDrawables(ctx.star(), drawables);
            }
            return new JsonObject(drawables.entrySet().stream().collect(Collectors.toMap(
                    Map.Entry::getKey,
                    kv -> JsonObject.mapFrom(kv.getValue()),
                    (s, a) -> s)));
        }
    }
}
