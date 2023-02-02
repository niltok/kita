package ikuyo.server.renderers;

import ikuyo.api.Drawable;
import ikuyo.api.Star;
import ikuyo.server.api.Renderer;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface DrawablesRenderer extends Renderer {
    default JsonObject render(Star star) {
        var drawables = new ArrayList<Drawable>();
        renderDrawables(star, drawables);
        return genDrawables(drawables);
    }

    private static JsonObject genDrawables(ArrayList<Drawable> drawables) {
        return new JsonObject(drawables.stream().map(JsonObject::mapFrom).collect(Collectors.toMap(
                json -> {
                    var key = json.getString("key");
                    json.putNull("key");
                    var hash = String.valueOf(json.hashCode());
                    json.put("key", key);
                    return hash;
                },
                Function.identity(),
                (s, a) -> s
        )));
    }

    void renderDrawables(Star star, List<Drawable> drawables);

    class Composite implements Renderer {
        DrawablesRenderer[] renderers;
        public Composite(DrawablesRenderer... renderers) {
            this.renderers = renderers;
        }

        @Override
        public JsonObject render(Star star) {
            var drawables = new ArrayList<Drawable>();
            for (DrawablesRenderer renderer : renderers) {
                renderer.renderDrawables(star, drawables);
            }
            return genDrawables(drawables);
        }
    }
}
