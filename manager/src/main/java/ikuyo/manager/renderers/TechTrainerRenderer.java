package ikuyo.manager.renderers;

import ikuyo.api.TechItem;
import ikuyo.api.UIElement;
import ikuyo.manager.api.CommonContext;
import io.vertx.core.json.JsonObject;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TechTrainerRenderer implements UIRenderer {
    @Override
    public void renderUI(CommonContext context, Map<Integer, List<UIElement>> result) {
        for (Integer id : context.updated().users()) {
            var ui = result.computeIfAbsent(id, i -> new ArrayList<>());
            if (!context.userState().get(id).techTrainerDisplay) {
                ui.add(new UIElement("div").withClass("placeholder"));
                continue;
            }
            ui.add(new UIElement("div", techList(context, id), queueList(context, id))
                    .withClass("popout-container", "tech-trainer-container", "background"));
        }
    }

    public UIElement techList(CommonContext context, int id) {
        var tree = context.userState().get(id).user.techTree();
        return new UIElement("div", TechItem.techList.stream().filter(TechItem::isEnable).map(tech -> {
            var state = tree.getTechStatus(tech);
            var data = tree.getData(tech);
            var stateStr = new StringBuilder(30);
            var displayPercent = data.trainPercent != 0 && data.trainPercent != 1;
            if (displayPercent || state == 0) {
                stateStr.append("%.1f%%".formatted(data.trainPercent * 100));
            }
            if (state == 0 || state == -1 && displayPercent)
                stateStr.append("/");
            stateStr.append(switch (state) {
                case 1 -> "已完成";
                case 0 -> "研究中";
                case -1 -> "队列中";
                default -> "";
            });
            var style = JsonObject.of();
            var callback = JsonObject.of();
            if (state == -2) {
                style.put("cursor", "pointer");
                callback.put("type", "techTrainer.add");
                callback.put("tech", tech.name());
            }
            return new UIElement.Callback("div", callback,
                    new UIElement("span", new UIElement.Text(tech.displayName)),
                    new UIElement("span", new UIElement.Text(stateStr.toString()))
            ).withClass("tech-tree-item", "hover-label").withStyle(style);
        }).toArray(UIElement[]::new)).withClass("tech-tree", "auto-flow-container");
    }

    public UIElement queueList(CommonContext context, int id) {
        var tree = context.userState().get(id).user.techTree();
        return new UIElement("div", Arrays.stream(tree.getTrainQueue()).map(e -> {
            var tech = TechItem.get(e);
            assert tech != null;
            var state = tree.getTechStatus(tech);
            var data = tree.getData(tech);
            var stateStr = "";
            var time = Instant.ofEpochMilli(data.trainFinishAt);
            stateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(time);
            return new UIElement("div",
                    new UIElement("span", new UIElement.Text(tech.displayName)),
                    new UIElement("span", new UIElement.Text(stateStr))
            ).withClass("hover-label");
        }).toArray(UIElement[]::new)).withClass("queue-list", "auto-flow-container");
    }
}
