package ikuyo.manager.renderers;

import ikuyo.api.datatypes.UIElement;
import ikuyo.api.renderers.UIRenderer;
import ikuyo.manager.api.CommonContext;

import java.util.Map;
import java.util.Queue;

public class CargoRenderer implements UIRenderer<CommonContext> {
    @Override
    public void renderUI(CommonContext context, Map<Integer, Queue<UIElement>> result) {
        context.updated().users().forEach((id) -> {
            var info = context.getInfo(id);
            var state = context.getState(id);
            if (info == null || state == null || !"cargoHold".equals(state.page) || state.pageEdge < 2) {
                return;
            }
            var ui = result.computeIfAbsent(id, i -> UIRenderer.emptyQueue());
            ui.add(UIElement.div(
                    UIElement.div(
                            state.stationCargo.renderUI("空间站", "ship.load"),
                            info.spaceship.cargoHold.renderUI("货舱", "ship.unload")
                    ).appendClass("multi-column", "auto-expand", "auto-flow-container")
            ).appendClass("popout-container", "flex-box-container", "background"));
        });
    }
}
