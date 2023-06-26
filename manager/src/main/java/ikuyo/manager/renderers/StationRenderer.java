package ikuyo.manager.renderers;

import ikuyo.api.datatypes.UIElement;
import ikuyo.api.renderers.UIRenderer;
import ikuyo.manager.api.CommonContext;
import io.vertx.core.json.JsonObject;

import java.util.Map;
import java.util.Queue;

public class StationRenderer implements UIRenderer<CommonContext> {
    @Override
    public void renderUI(CommonContext context, Map<Integer, Queue<UIElement>> result) {
        for (Integer id : context.updated().users()) {
            var state = context.getState(id);
            if (state == null || !state.inStation()) continue;
            var ui = result.computeIfAbsent(id, i -> UIRenderer.emptyQueue());
            ui.add(new UIElement("div", stationUI())
                    .appendClass("fullscreen", "station-background", "absolute"));
        }
    }

    UIElement stationUI() {
        return UIElement.div(UIElement.div(undockButton()).appendClass("center-bottom"));
    }

    UIElement undockButton() {
        return UIElement.callbackText("出站", JsonObject.of("type", "user.move.undock"));
    }
}
