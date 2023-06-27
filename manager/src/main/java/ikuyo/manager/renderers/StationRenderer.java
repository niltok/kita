package ikuyo.manager.renderers;

import ikuyo.api.cargo.CargoItem;
import ikuyo.api.cargo.UnpackItem;
import ikuyo.api.datatypes.UIElement;
import ikuyo.api.datatypes.UserInfo;
import ikuyo.api.renderers.UIRenderer;
import ikuyo.api.spaceships.Spaceship;
import ikuyo.manager.api.CommonContext;
import ikuyo.manager.api.UserState;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class StationRenderer implements UIRenderer<CommonContext> {
    @Override
    public void renderUI(CommonContext context, Map<Integer, Queue<UIElement>> result) {
        for (Integer id : context.updated().users()) {
            var state = context.getState(id);
            var info = context.getInfo(id);
            if (state == null || info == null || !state.inStation()) continue;
            var ui = result.computeIfAbsent(id, i -> UIRenderer.emptyQueue());
            ui.add(new UIElement("div", stationUI(state, info))
                    .appendClass("fullscreen", "station-background", "absolute"));
        }
    }

    UIElement stationUI(UserState state, UserInfo info) {
        return UIElement.div(
                shipSwitcher(state, info).appendClass("center-right", "background"),
                UIElement.div(undockButton()).appendClass("center-bottom")
        );
    }

    UIElement shipSwitcher(UserState state, UserInfo info) {
        var uis = new ArrayList<UIElement>();
        uis.add(UIElement.titleLabel(info.spaceship.name).appendClass("active"));
        List<UnpackItem> unpacks = state.stationCargo.unpacks;
        for (int i = 0, unpacksSize = unpacks.size(); i < unpacksSize; i++) {
            UnpackItem unpack = unpacks.get(i);
            if (!(unpack instanceof Spaceship ship)) continue;
            uis.add(UIElement.labelItem(
                    UIElement.text(ship.name),
                    UIElement.text(""),
                    JsonObject.of("type", "ship.switch", "index", i)
            ).appendClass("hover-label"));
        }
        state.stationCargo.items.forEach((type, num) -> {
            var item = CargoItem.get(type);
            if (item == null || item.unpackClass == null || !Spaceship.class.isAssignableFrom(item.unpackClass))
                return;
            uis.add(UIElement.labelItem(
                    UIElement.text(item.displayName),
                    UIElement.text("数量: %d".formatted(num)),
                    JsonObject.of("type", "ship.switch", "key", type)
            ).appendClass("hover-label"));
        });
        return UIElement.div(uis.toArray(UIElement[]::new));
    }

    UIElement undockButton() {
        return UIElement.callbackText("出站", JsonObject.of("type", "user.move.undock"));
    }
}
