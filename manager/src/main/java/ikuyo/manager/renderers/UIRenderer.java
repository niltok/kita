package ikuyo.manager.renderers;

import ikuyo.api.UIElement;
import ikuyo.api.renderers.Renderer;
import ikuyo.manager.api.CommonContext;
import ikuyo.utils.AsyncHelper;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface UIRenderer extends AsyncHelper {
    void renderUI(CommonContext context, Map<Integer, List<UIElement>> result);

    class Composite implements Renderer<CommonContext> {
        UIRenderer[] renderers;
        public Composite(UIRenderer... renderers) {
            this.renderers = renderers;
        }
        @Override
        public JsonObject render(CommonContext context) {
            Map<Integer, List<UIElement>> map = new HashMap<>();
            for (var renderer : renderers) {
                renderer.renderUI(context, map);
            }
            JsonObject res = JsonObject.of();
            map.forEach((i, us) -> {
                res.put(String.valueOf(i),
                        JsonObject.of("ui", JsonObject.mapFrom(
                                new UIElement("div", us.toArray(UIElement[]::new))
                                        .withClass("absolute", "fullscreen", "pointer-pass")
                        )));
            });
            return res;
        }
    }
}
