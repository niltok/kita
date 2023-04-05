package ikuyo.manager.renderers;

import ikuyo.api.UIElement;
import ikuyo.api.renderers.UIRenderer;
import ikuyo.api.techtree.TechItem;
import ikuyo.api.techtree.TechLevel;
import ikuyo.api.techtree.TechTree;
import ikuyo.manager.api.CommonContext;
import io.vertx.core.json.JsonObject;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TechTrainerRenderer implements UIRenderer<CommonContext> {
    @Override
    public void renderUI(CommonContext context, Map<Integer, List<UIElement>> result) {
        for (Integer id : context.updated().users()) {
            var state = context.getState(id);
            if (state == null || !"techTrainer".equals(state.page) || state.pageEdge < 2) continue;
            var ui = result.computeIfAbsent(id, i -> new ArrayList<>());
            ui.add(new UIElement("div", displayPoint(context, id), techList(context, id))
                    .withClass("popout-container", "flex-box-container", "background"));
        }
    }

    public UIElement displayPoint(CommonContext context, int id) {
        var tree = context.userState().get(id).user.techTree();
        var rightText = new StringBuilder();
        rightText.append("Available: ").append(displayDuration(Duration.ofMillis(tree.availableTime())));
        return new UIElement("div",
                new UIElement("span"),
                new UIElement("span", new UIElement.Text(rightText.toString()))
        ).withClass("label-item", "tech-trainer-header");
    }

    public UIElement techList(CommonContext context, int id) {
        var tree = context.userState().get(id).user.techTree();
        return new UIElement("div", TechItem.techList.stream().filter(tech -> tech.enable).map(tech -> {
            var data = tree.treeInfo.computeIfAbsent(tech.name(), i -> new TechTree.Data());
            var stateStr = new StringBuilder(30);
            if (data.level < tech.maxLevel) {
                var cost = tech.cost.apply(data.level + 1);
                if (cost.isZero()) stateStr.append("Free");
                else stateStr.append("Need ").append(displayDuration(cost));
                if (data.level != 0) {
                    stateStr.append(" / Level (").append(data.level).append(" / ").append(tech.maxLevel).append(")");
                }
            } else {
                stateStr.append("Max Level");
            }
            JsonObject callback = null;
            if (data.level < tech.maxLevel && tree.canTrain(new TechLevel(tech, data.level + 1))) {
                callback = JsonObject.of(
                        "type", "techTrainer.train",
                        "tech", tech.name(),
                        "level", data.level + 1
                );
            }
            return UIElement.labelItem(
                    new UIElement.Text(tech.displayName),
                    new UIElement.Text(stateStr.toString()),
                    callback
            ).appendClass("hover-label").withTitle(tech.description);
        }).toArray(UIElement[]::new))
                .withClass("tech-tree", "column2", "auto-expand", "auto-flow-container");
    }

    public static String displayDuration(Duration duration) {
        var sb = new StringBuilder(30);
        if (duration.toDays() > 0) sb.append(duration.toDays()).append("天");
        if (duration.toDays() < 3 && duration.toHoursPart() > 0) sb.append(duration.toHoursPart()).append("时");
        if (duration.toHours() < 3 && duration.toMinutesPart() > 0) sb.append(duration.toMinutesPart()).append("分");
        if (duration.toMinutes() < 10 && duration.toSecondsPart() > 0) sb.append(duration.toSecondsPart()).append("秒");
        return sb.toString();
    }
}
