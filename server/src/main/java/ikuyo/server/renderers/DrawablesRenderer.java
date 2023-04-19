package ikuyo.server.renderers;

import ikuyo.api.datatypes.Drawable;
import ikuyo.api.renderers.Renderer;
import ikuyo.server.api.CommonContext;
import ikuyo.utils.WindowSum;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

import static ikuyo.api.behaviors.Behavior.windowSize;

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
        public Map<String, WindowSum> profilers = new HashMap<>();
        public Composite(DrawablesRenderer... renderers) {
            this.renderers = renderers;
        }

        @Override
        public JsonObject render(CommonContext ctx) {
            var drawables = new HashMap<String, Drawable>();
            for (DrawablesRenderer renderer : renderers) {
                double startTime = System.nanoTime();
                try {
                    renderer.renderDrawables(ctx, drawables);
                } catch (Exception e) {
                    System.err.println(e.getLocalizedMessage());
                    e.printStackTrace();
                } finally {
                    profilers.computeIfAbsent(renderer.getClass().getSimpleName(), i -> new WindowSum(windowSize))
                            .put(System.nanoTime() - startTime);
                }
            }
            var res = JsonObject.of();
            drawables.forEach((s, d) -> res.put(s, JsonObject.mapFrom(d)));
            return res;
        }
    }
}
