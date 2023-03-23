package ikuyo.server.renderers;

import ikuyo.api.StarInfo;
import ikuyo.api.UIElement;
import ikuyo.api.renderers.UIRenderer;
import ikuyo.server.api.CommonContext;
import ikuyo.server.api.PhysicsEngine;
import org.dyn4j.geometry.Ray;
import org.dyn4j.geometry.Vector2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserStateRenderer implements UIRenderer<CommonContext> {
    @Override
    public void renderUI(CommonContext context, Map<Integer, List<UIElement>> result) {
        context.star().starInfo().starUsers.forEach((id, info) -> {
            var ui = result.computeIfAbsent(id, i -> new ArrayList<>());
            if ("fly".equals(info.controlType)) {
                ui.add(getHeightInfo(context, info));
            }
            else ui.add(new UIElement("div").withClass("placeholder"));
            ui.add(new UIElement("div",
                    new UIElement.Text("HP: %.0f".formatted(info.HP))
            ).withClass("left-bottom", "pointer-pass-all"));
        });
    }

    private static UIElement getHeightInfo(CommonContext context, StarInfo.StarUserInfo info) {
        var height = context.engine().rayCast(
                        new Ray(new Vector2(info.x, info.y), new Vector2(-info.x, -info.y)),
                        Math.hypot(info.x, info.y), filter -> filter.equals(PhysicsEngine.BLOCK))
                .get(0).copy().getRaycast().getPoint().subtract(new Vector2(info.x, info.y)).getMagnitude();
        return new UIElement("div",
                new UIElement.Text("Level: %.1f".formatted(Math.hypot(info.x, info.y))),
                new UIElement("br"),
                new UIElement.Text("Height: %.1f".formatted(height)))
                .withClass("center-top", "pointer-pass-all");
    }
}

