package ikuyo.server.renderers;

import ikuyo.api.datatypes.UIElement;
import ikuyo.api.renderers.UIRenderer;
import ikuyo.server.api.CommonContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CargoRenderer implements UIRenderer<CommonContext> {
    @Override
    public void renderUI(CommonContext context, Map<Integer, List<UIElement>> result) {
        context.updated().users().forEach((id) -> {
            var info = context.getInfo(id);
            var state = context.getState(id);
            if (info == null || !info.online || !"cargoHold".equals(state.page)) {
                return;
            }
            var ui = result.computeIfAbsent(id, i -> new ArrayList<>());
            ui.add(new UIElement("div",
                    info.spaceship.cargoHold.renderUI()
                            .appendClass("column2", "auto-expand", "auto-flow-container")
            ).appendClass("popout-container", "flex-box-container", "background"));
        });
    }
}
