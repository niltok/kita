package ikuyo.server.renderers;

import ikuyo.api.datatypes.UIElement;
import ikuyo.api.datatypes.UserInfo;
import ikuyo.api.renderers.UIRenderer;
import ikuyo.api.spaceships.Spaceship;
import ikuyo.server.api.CommonContext;
import ikuyo.server.api.UserState;
import ikuyo.utils.CPUMonitor;
import ikuyo.utils.StarUtils;

import java.util.ArrayList;
import java.util.Map;
import java.util.Queue;

public class UserStateRenderer implements UIRenderer<CommonContext> {
    @Override
    public void renderUI(CommonContext context, Map<Integer, Queue<UIElement>> result) {
        context.updated().users().forEach((id) -> {
            var info = context.getInfo(id);
            if (info == null || !info.online) return;
            var ui = result.computeIfAbsent(id, i -> UIRenderer.emptyQueue());
            var ship = info.spaceship;
            if ("fly".equals(info.controlType)) {
                ui.add(getHeightInfo(context, info, id));
            }
            ui.add(getShipInfo(ship));
            if (!ship.weapons.isEmpty()) ui.add(getWeaponInfo(ship));
            var state = context.getState(id);
            if (state != null /*&& state.user.isAdmin()*/) ui.add(getAdminInfo(context, state));
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
        ).appendClass("left-bottom", "pointer-pass-all", "background");
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
                .appendClass("right-bottom", "pointer-pass-all", "background");
    }

    private static UIElement getHeightInfo(CommonContext context, UserInfo info, int id) {
        return new UIElement("div",
                UIElement.labelItem("Level", "%.1f".formatted(Math.hypot(info.x, info.y)))
                        .appendClass("normal-label"),
                UIElement.labelItem("Height", "%.1f".formatted(context.engine().users.get(id).groundClearance))
                        .appendClass("normal-label")
        ).appendClass("center-top", "pointer-pass-all", "background");
    }

    private static UIElement getAdminInfo(CommonContext context, UserState state) {
        var pointer = state.input.pointAt;
        int realIndex = StarUtils.realIndexOf(pointer.x, pointer.y);
        var area = StarUtils.getAreaOf(realIndex);
        var cpuLoad = CPUMonitor.instance.getProcessCpu() * 100.;
        var runtime = Runtime.getRuntime();
        var memLoad = (runtime.totalMemory() - runtime.freeMemory()) / 1024. / 1024.;
        return new UIElement("div",
                UIElement.labelItem("Pointer", "(%.1f, %.1f)".formatted(pointer.x, pointer.y))
                        .appendClass("normal-label"),
                UIElement.labelItem("Index[Real]", "%d[%d] | %d".formatted(StarUtils.indexOf(realIndex), realIndex, StarUtils.tierOf(realIndex)))
                        .appendClass("normal-label"),
                UIElement.labelItem("Area | Tier", "%d | %d".formatted(area, StarUtils.tierOf(area)))
                        .appendClass("normal-label"),
                UIElement.labelItem("Time(Upd | Δ)", "%.1f | %.1f".formatted(
                        context.update.getMean(), context.delta.getMean()))
                        .appendClass("normal-label"),
                UIElement.labelItem("CPU | Mem", "%.1f%% | %.1fM".formatted(cpuLoad, memLoad))
                        .appendClass("normal-label")
        ).appendClass("right-top", "pointer-pass-all", "background");
    }
}

