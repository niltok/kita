package ikuyo.server.renderers;

import ikuyo.api.UIElement;
import ikuyo.api.renderers.UIRenderer;
import ikuyo.server.api.CommonContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CargoRenderer implements UIRenderer<CommonContext> {
    @Override
    public void renderUI(CommonContext context, Map<Integer, List<UIElement>> result) {
        context.getInfos().forEach((id, info) -> {
            var state = context.getState(id);
            if (!info.online || !"cargoHold".equals(state.page)) {
                return;
            }
            var ui = result.computeIfAbsent(id, i -> new ArrayList<>());
            ui.add(new UIElement("div",
                    info.spaceship.cargoHold.renderUI()
            ).withClass("popout-container", "flex-box-container", "background"));
        });
    }
}
