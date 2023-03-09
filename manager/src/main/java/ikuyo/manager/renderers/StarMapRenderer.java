package ikuyo.manager.renderers;

import ikuyo.api.UIElement;
import ikuyo.manager.api.RendererContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StarMapRenderer implements UIRenderer {
    @Override
    public void renderUI(RendererContext context, Map<Integer, List<UIElement>> result) {
        for (Integer id : context.common().updated().users()) {
            var ui = result.computeIfAbsent(id, i -> new ArrayList<>());
            if (!context.common().userState().get(id).mapDisplay) continue;
            ui.add(new UIElement("div").withClass("starmap-container", "background"));
        }
    }
}
