package ikuyo.server.renderers;

import ikuyo.api.StarInfo;
import ikuyo.api.UIElement;
import ikuyo.api.renderers.UIRenderer;
import ikuyo.api.spaceships.AbstractSpaceship;
import ikuyo.server.api.CommonContext;
import ikuyo.server.api.PhysicsEngine;
import org.dyn4j.geometry.Ray;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.result.RaycastResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserStateRenderer implements UIRenderer<CommonContext> {
    @Override
    public void renderUI(CommonContext context, Map<Integer, List<UIElement>> result) {
        context.updated().users().forEach((id) -> {
            var info = context.getInfo(id);
            if (info == null || !info.online) return;
            var ui = result.computeIfAbsent(id, i -> new ArrayList<>());
            var ship = info.spaceship;
            if ("fly".equals(info.controlType)) {
                ui.add(getHeightInfo(context, info));
            }
            ui.add(getShipInfo(ship));
            if (!ship.weapons.isEmpty()) ui.add(getWeaponInfo(ship));
        });
    }

    private static UIElement getShipInfo(AbstractSpaceship ship) {
        return new UIElement("div",
                UIElement.labelItem("Shield", "%.0f".formatted(ship.shield))
                        .appendClass("normal-label"),
                UIElement.labelItem("HP", "%.0f".formatted(ship.hp))
                        .appendClass("normal-label")
        ).withClass("left-bottom", "pointer-pass-all", "background");
    }

    private static UIElement getWeaponInfo(AbstractSpaceship ship) {
        var ui = new ArrayList<UIElement>();
        var current = ship.getCurrentWeapon();
        ship.weapons.forEach(weapon -> {
            ui.add(UIElement.labelItem(weapon.getInfo().displayName, weapon.getItemInfo())
                    .appendClass(current == weapon ? "focus-label" : "normal-label"));
        });
        return new UIElement("div", ui.toArray(UIElement[]::new))
                .withClass("right-bottom", "pointer-pass-all", "background");
    }

    private static UIElement getHeightInfo(CommonContext context, StarInfo.StarUserInfo info) {
        var height = context.engine().rayCast(
                        new Ray(new Vector2(info.x, info.y), new Vector2(-info.x, -info.y)),
                        Math.hypot(info.x, info.y), filter -> filter.equals(PhysicsEngine.BLOCK))
                .stream().min(RaycastResult::compareTo).get().copy().getRaycast().getDistance();
        return new UIElement("div",
                UIElement.labelItem("Level", "%.1f".formatted(Math.hypot(info.x, info.y)))
                        .appendClass("normal-label"),
                UIElement.labelItem("Height", "%.1f".formatted(height))
                        .appendClass("normal-label")
        ).withClass("center-top", "pointer-pass-all", "background");
    }
}

