package ikuyo.server.renderers;

import ikuyo.api.datatypes.UIElement;
import ikuyo.api.renderers.UIRenderer;
import ikuyo.server.api.CommonContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminPanelRenderer implements UIRenderer<CommonContext> {
    static boolean adminOnly = false;
    @Override
    public void renderUI(CommonContext context, Map<Integer, List<UIElement>> result) {
        context.updated().users().forEach((id) -> {
            var info = context.getInfo(id);
            var state = context.getState(id);
            if (info == null || !info.online || state == null || adminOnly && !state.user.isAdmin()
                    || !"adminPanel".equals(state.page)) {
                return;
            }
            var ui = result.computeIfAbsent(id, i -> new ArrayList<>());
            ui.add(new UIElement("div",
                    getPerformanceInfo(context),
                    getStarUserInfo(context)
            ).withClass("popout-container", "flex-box-container", "background", "auto-flow-container"));
        });
    }

    UIElement getPerformanceInfo(CommonContext context) {
        var uis = new ArrayList<UIElement>();
        context.profiles.forEach((name, time) -> {
            uis.add(UIElement.hoverLabel(name, "%.3f".formatted(time / 1000_000)));
        });
        return new UIElement("div",
                UIElement.normalLabel(new UIElement.Text("Star Performance").withClass("serif"),
                        "%.3f | %.3f".formatted(context.update.getMean(), context.delta.getMean())),
                new UIElement("div", uis.toArray(UIElement[]::new)).withClass("column2"));
    }

    UIElement getStarUserInfo(CommonContext context) {
        var uis = new ArrayList<UIElement>();
        context.star().starInfo().starUsers.forEach((id, info) -> {
            var state = context.getState(id);
            if (!info.online || state == null) return;
            uis.add(UIElement.hoverLabel(state.user.name(),
                    "id: %d%s".formatted(id, state.user.isAdmin() ? " | Admin" : "")));
        });
        return new UIElement("div",
                UIElement.normalLabel(new UIElement.Text("Star User").withClass("serif"),""),
                new UIElement("div", uis.toArray(UIElement[]::new)).withClass("column2"));
    }
}
