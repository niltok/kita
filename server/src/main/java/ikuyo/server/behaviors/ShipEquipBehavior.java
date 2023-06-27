package ikuyo.server.behaviors;

import ikuyo.api.behaviors.Behavior;
import ikuyo.server.api.CommonContext;

import java.util.Map;

public class ShipEquipBehavior implements Behavior<CommonContext> {
    @Override
    public void update(CommonContext context) {
        context.updated().users().forEach(id -> {
            var state = context.getState(id);
            var info = context.getInfo(id);
            if (state == null || info == null) return;
            info.spaceship.handleUserEvents(state.events, Map.of(), "");
        });
    }
}
