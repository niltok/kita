package ikuyo.manager.renderers;

import ikuyo.api.UIElement;
import ikuyo.api.renderers.UIRenderer;
import ikuyo.api.techtree.TechItem;
import ikuyo.api.techtree.TechLevel;
import ikuyo.manager.api.CommonContext;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TechTrainerRenderer implements UIRenderer<CommonContext> {
    @Override
    public void renderUI(CommonContext context, Map<Integer, List<UIElement>> result) {
        for (Integer id : context.updated().users()) {
            var ui = result.computeIfAbsent(id, i -> new ArrayList<>());
            if (!"techTrainer".equals(context.userState().get(id).page)) {
                ui.add(new UIElement("div").withClass("placeholder"));
                continue;
            }
            ui.add(new UIElement("div", techList(context, id))
                    .withClass("popout-container", "tech-trainer-container", "background"));
        }
    }

    public UIElement techList(CommonContext context, int id) {
        var tree = context.userState().get(id).user.techTree();
        return new UIElement("div", TechItem.techList.stream().filter(TechItem::isEnable).map(tech -> {
            var data = tree.treeInfo.get(tech.name());
            var stateStr = new StringBuilder(30);
            if (data != null && data.level != 0) {
                stateStr.append("Level ").append(data.level);
            }
            var style = JsonObject.of();
            var callback = JsonObject.of();
            if (data == null || data.level < tech.maxLevel
                    && tree.canTrain(new TechLevel(tech, data.level + 1))) {
                style.put("cursor", "pointer");
                callback.put("type", "techTrainer.train");
                callback.put("tech", tech.name());
                callback.put("level", data == null ? 1 : data.level + 1);
            }
            return new UIElement.Callback("div", callback,
                    new UIElement("span", new UIElement.Text(tech.displayName)),
                    new UIElement("span", new UIElement.Text(stateStr.toString()))
            ).withClass("tech-tree-item", "hover-label").withStyle(style);
        }).toArray(UIElement[]::new)).withClass("tech-tree", "auto-flow-container");
    }
}
