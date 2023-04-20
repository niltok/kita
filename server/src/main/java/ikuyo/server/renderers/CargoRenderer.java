package ikuyo.server.renderers;

import ikuyo.api.datatypes.UIElement;
import ikuyo.api.renderers.UIRenderer;
import ikuyo.server.api.CommonContext;

import java.util.Map;
import java.util.Queue;

public class CargoRenderer implements UIRenderer<CommonContext> {
    @Override
    public void renderUI(CommonContext context, Map<Integer, Queue<UIElement>> result) {
        context.updated().users().forEach((id) -> {
            var info = context.getInfo(id);
            var state = context.getState(id);
            if (info == null || !info.online || !"cargoHold".equals(state.page)) {
                return;
            }
            var ui = result.computeIfAbsent(id, i -> UIRenderer.emptyQueue());
            ui.add(new UIElement("div",
                    info.spaceship.cargoHold.renderUI()
                            .appendClass("column2", "auto-expand", "auto-flow-container")
            ).appendClass("popout-container", "flex-box-container", "background"));
        });
    }
}
