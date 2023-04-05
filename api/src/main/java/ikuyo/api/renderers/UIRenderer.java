package ikuyo.api.renderers;

import ikuyo.api.datatypes.UIElement;
import ikuyo.utils.AsyncHelper;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface UIRenderer<T> extends AsyncHelper {
    void renderUI(T context, Map<Integer, List<UIElement>> result);

    class Composite<T> implements Renderer<T> {
        UIRenderer<T>[] renderers;
        @SafeVarargs
        public Composite(UIRenderer<T>... renderers) {
            this.renderers = renderers;
        }
        @Override
        public JsonObject render(T context) {
            Map<Integer, List<UIElement>> map = new HashMap<>();
            for (var renderer : renderers) {
                try {
                    renderer.renderUI(context, map);
                } catch (Exception e) {
                    System.err.println(e.getLocalizedMessage());
                    e.printStackTrace();
                }
            }
            JsonObject res = JsonObject.of();
            map.forEach((i, us) -> {
                res.put(String.valueOf(i), JsonObject.mapFrom(
                                new UIElement("div", us.toArray(UIElement[]::new))
                                        .withClass("absolute", "fullscreen", "pointer-pass")
                        ));
            });
            return res;
        }
    }
}
