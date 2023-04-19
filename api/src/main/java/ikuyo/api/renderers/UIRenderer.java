package ikuyo.api.renderers;

import ikuyo.api.datatypes.UIElement;
import ikuyo.utils.AsyncHelper;
import ikuyo.utils.WindowSum;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ikuyo.api.behaviors.Behavior.windowSize;

public interface UIRenderer<T> extends AsyncHelper {
    void renderUI(T context, Map<Integer, List<UIElement>> result);

    class Composite<T> implements Renderer<T> {
        UIRenderer<T>[] renderers;
        public Map<String, WindowSum> profilers = new HashMap<>();
        @SafeVarargs
        public Composite(UIRenderer<T>... renderers) {
            this.renderers = renderers;
        }
        @Override
        public JsonObject render(T context) {
            Map<Integer, List<UIElement>> map = new HashMap<>();
            for (var renderer : renderers) {
                double startTime = System.nanoTime();
                try {
                    renderer.renderUI(context, map);
                } catch (Exception e) {
                    System.err.println(e.getLocalizedMessage());
                    e.printStackTrace();
                } finally {
                    profilers.computeIfAbsent(renderer.getClass().getSimpleName(), i -> new WindowSum(windowSize))
                            .put(System.nanoTime() - startTime);
                }
            }
            JsonObject res = JsonObject.of();
            map.forEach((i, us) -> {
                res.put(String.valueOf(i), JsonObject.mapFrom(
                                new UIElement("div", us.toArray(UIElement[]::new))
                                        .appendClass("absolute", "fullscreen", "pointer-pass")
                        ));
            });
            return res;
        }
    }
}
