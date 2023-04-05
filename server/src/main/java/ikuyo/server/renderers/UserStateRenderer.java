package ikuyo.server.renderers;

import ikuyo.api.datatypes.UIElement;
import ikuyo.api.datatypes.UserInfo;
import ikuyo.api.renderers.UIRenderer;
import ikuyo.api.spaceships.Spaceship;
import ikuyo.server.api.CommonContext;
import ikuyo.server.api.PhysicsEngine;
import ikuyo.server.api.UserState;
import ikuyo.utils.StarUtils;
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
            var state = context.getState(id);
            if (state != null && state.user.isAdmin()) ui.add(getAdminInfo(context, state));
        });
    }

    private static UIElement getShipInfo(Spaceship ship) {
        return new UIElement("div",
                UIElement.labelItem("Shield", "%.0f".formatted(ship.shield),
                                ship.shield / ship.getMaxShield())
                        .appendClass("normal-label"),
                UIElement.labelItem("HP", "%.0f".formatted(ship.hp),
                                ship.hp / ship.getMaxHp())
                        .appendClass("normal-label")
        ).withClass("left-bottom", "pointer-pass-all", "background");
    }

    private static UIElement getWeaponInfo(Spaceship ship) {
        var ui = new ArrayList<UIElement>();
        var current = ship.getCurrentWeapon();
        ship.weapons.forEach(weapon -> {
            var percent = weapon.reloading ? (double) weapon.restActiveTime / weapon.getReloadingTime() :
                    weapon.restActiveTime == 0 ?
                            (double) weapon.ammoAmount / weapon.getAmmoMax() :
                            (double) weapon.restActiveTime / weapon.getActiveTime();
            ui.add(UIElement.labelItem(weapon.getInfo().displayName, weapon.getItemInfo(), percent)
                    .appendClass(current == weapon ? "focus-label" : "normal-label"));
        });
        return new UIElement("div", ui.toArray(UIElement[]::new))
                .withClass("right-bottom", "pointer-pass-all", "background");
    }

    private static UIElement getHeightInfo(CommonContext context, UserInfo info) {
        var minHit = context.engine().rayCast(
                        new Ray(new Vector2(info.x, info.y), new Vector2(-info.x, -info.y)),
                        Math.hypot(info.x, info.y), filter -> filter.equals(PhysicsEngine.BLOCK))
                .stream().min(RaycastResult::compareTo);
        var height = minHit.map(res -> res.getRaycast().getDistance()).orElse(Double.NaN);
        return new UIElement("div",
                UIElement.labelItem("Level", "%.1f".formatted(Math.hypot(info.x, info.y)))
                        .appendClass("normal-label"),
                UIElement.labelItem("Height", "%.1f".formatted(height))
                        .appendClass("normal-label")
        ).withClass("center-top", "pointer-pass-all", "background");
    }

    private static UIElement getAdminInfo(CommonContext context, UserState state) {
        var pointer = state.input.pointAt;
        int realIndex = StarUtils.realIndexOf(pointer.x, pointer.y);
        var area = StarUtils.getAreaOf(realIndex);
        return new UIElement("div",
                UIElement.labelItem("Pointer", "(%.1f, %.1f)".formatted(pointer.x, pointer.y))
                        .appendClass("normal-label"),
                UIElement.labelItem("Index[Real]", "%d[%d]".formatted(StarUtils.indexOf(realIndex), realIndex))
                        .appendClass("normal-label"),
                UIElement.labelItem("Area", "%d".formatted(area))
                        .appendClass("normal-label"),
                UIElement.labelItem("Time(Update | Delta)", "%.1f | %.1f".formatted(
                        context.update.getMean(), context.delta.getMean()))
                        .appendClass("normal-label")
        ).withClass("right-top", "pointer-pass-all", "background");
    }
}

