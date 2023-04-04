package ikuyo.server.renderers;

import ikuyo.api.UIElement;
import ikuyo.api.UserInfo;
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
                UIElement.labelItem("Shield", "%.0f".formatted(ship.shield),
                                ship.shield / ship.getMaxShield())
                        .appendClass("normal-label"),
                UIElement.labelItem("HP", "%.0f".formatted(ship.hp),
                                ship.hp / ship.getMaxHp())
                        .appendClass("normal-label")
        ).withClass("left-bottom", "pointer-pass-all", "background");
    }

    private static UIElement getWeaponInfo(AbstractSpaceship ship) {
        var ui = new ArrayList<UIElement>();
        var current = ship.getCurrentWeapon();
        ship.weapons.forEach(weapon -> {
            var percent = weapon.reloading ? (double) weapon.restFireTime / weapon.getReloadingTime() :
                    weapon.restFireTime == 0 ?
                            (double) weapon.ammoAmount / weapon.getAmmoMax() :
                            (double) weapon.restFireTime / weapon.getFireTime();
            ui.add(UIElement.labelItem(weapon.getInfo().displayName, weapon.getItemInfo(), percent)
                    .appendClass(current == weapon ? "focus-label" : "normal-label"));
        });
        return new UIElement("div", ui.toArray(UIElement[]::new))
                .withClass("right-bottom", "pointer-pass-all", "background");
    }

    private static UIElement getHeightInfo(CommonContext context, UserInfo info) {
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

