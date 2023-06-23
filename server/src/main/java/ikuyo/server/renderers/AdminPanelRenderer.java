package ikuyo.server.renderers;

import ikuyo.api.datatypes.UIElement;
import ikuyo.api.renderers.UIRenderer;
import ikuyo.server.api.AreaState;
import ikuyo.server.api.CommonContext;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.Queue;

public class AdminPanelRenderer implements UIRenderer<CommonContext> {
    static boolean adminOnly = false;
    @Override
    public void renderUI(CommonContext context, Map<Integer, Queue<UIElement>> result) {
        context.updated().users().forEach((id) -> {
            var info = context.getInfo(id);
            var state = context.getState(id);
            if (info == null || !info.online || state == null || adminOnly && !state.user.isAdmin()
                    || !"adminPanel".equals(state.page)) {
                return;
            }
            var ui = result.computeIfAbsent(id, i -> UIRenderer.emptyQueue());
            ui.add(new UIElement("div",
                    userAdd(),
                    getPerformanceInfo(context),
                    getStarUserInfo(context)
            ).appendClass("popout-container", "flex-box-container", "background", "auto-flow-container"));
        });
    }

    UIElement userAdd() {
        var uis = new ArrayList<UIElement>();
        uis.add(UIElement.div(
                new UIElement.Stateful("input.number", "shadow-add-num", "1")
                        .appendClass("auto-expand"),
                UIElement.callbackText("Add Shadow User", JsonObject.of("type", "user.add.shadow"),
                        "shadow-add-num")
        ).appendClass("label-item"));
        return new UIElement("div",
                UIElement.titleLabel("User Manager"),
                new UIElement("div", uis.toArray(UIElement[]::new)).appendClass());
    }

    UIElement getPerformanceInfo(CommonContext context) {
        var topComp = new ArrayList<UIElement>();
        var detail = new ArrayList<UIElement>();
        var sum = 0D;
        Comparator<Map.Entry<String, Double>> comparator = Map.Entry.comparingByValue();
        var perf = context.profiles.entrySet().stream().sorted(comparator.reversed()).limit(10).toList();
        for (Map.Entry<String, Double> e : perf) {
            sum += e.getValue() / 1000_000;
            topComp.add(UIElement.hoverLabel(e.getKey(), "%.3f".formatted(e.getValue() / 1000_000)));
        }
        detail.add(UIElement.hoverLabel("Update", "%.3f".formatted(context.update.getMean())));
        detail.add(UIElement.hoverLabel("Sum", "%.3f".formatted(sum)));
        detail.add(UIElement.hoverLabel("Delta", "%.3f".formatted(context.delta.getMean())));
        detail.add(UIElement.hoverLabel("Message", "%.3f".formatted(context.message.getMean())));
        detail.add(UIElement.hoverLabel("Frame", "%.3f".formatted(context.frameTime.getMean())));
        detail.add(UIElement.hoverLabel("Bodies[Dyn]", "%d[%d]".formatted(context.engine().bodyCount(),
                context.engine().dynBodyCount())));
        if (AreaState.workSet) {
            detail.add(UIElement.hoverLabel("DeltaAreas", "%d".formatted(context.areaDelta)));
            detail.add(UIElement.hoverLabel("EnabledAreas", "%d".formatted(context.enabledAreas.size())));
        }
        return new UIElement("div",
                UIElement.titleLabel("Star Performance"),
                new UIElement("div", detail.toArray(UIElement[]::new)).appendClass("column2"),
                new UIElement("div", topComp.toArray(UIElement[]::new)).appendClass("column2"));
    }

    UIElement getStarUserInfo(CommonContext context) {
        var uis = new ArrayList<UIElement>();
        var infos = context.getInfos();
        infos.forEach((id, info) -> {
            var state = context.getState(id);
            if (!info.online || state == null) return;
            uis.add(UIElement.hoverLabel(state.user.name(),
                    "id: %d%s".formatted(id, state.user.isAdmin() ? " | Admin" : "")));
        });
        return new UIElement("div",
                UIElement.titleLabel("Star User", "%d".formatted(infos.size())),
                new UIElement("div", uis.toArray(UIElement[]::new)).appendClass("column2"));
    }
}
