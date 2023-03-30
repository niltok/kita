package ikuyo.server.renderers;

import ikuyo.api.Drawable;
import ikuyo.api.renderers.Renderer;
import ikuyo.server.api.CommonContext;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public interface DrawablesRenderer extends Renderer<CommonContext> {
    default JsonObject render(CommonContext ctx) {
        var drawables = new HashMap<String, Drawable>();
        renderDrawables(ctx, drawables);
        var res = JsonObject.of();
        drawables.forEach((s, d) -> res.put(s, JsonObject.mapFrom(d)));
        return res;
    }

    void renderDrawables(CommonContext ctx, Map<String, Drawable> drawables);

    class Composite implements Renderer<CommonContext> {
        DrawablesRenderer[] renderers;
        public Composite(DrawablesRenderer... renderers) {
            this.renderers = renderers;
        }

        @Override
        public JsonObject render(CommonContext ctx) {
            var drawables = new HashMap<String, Drawable>();
            for (DrawablesRenderer renderer : renderers) {
                try {
                    renderer.renderDrawables(ctx, drawables);
                } catch (Exception e) {
                    System.err.println(e.getLocalizedMessage());
                    e.printStackTrace();
                }
            }
            var res = JsonObject.of();
            drawables.forEach((s, d) -> res.put(s, JsonObject.mapFrom(d)));
            return res;
        }
    }
}
