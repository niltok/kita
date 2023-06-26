package ikuyo.manager.behaviors;

import ikuyo.api.behaviors.Behavior;
import ikuyo.manager.api.CommonContext;

import java.util.HashMap;

public class ShipEquipBehavior implements Behavior<CommonContext> {
    @Override
    public void update(CommonContext context) {
        context.updated().users().forEach(id -> {
            var state = context.getState(id);
            var info = context.getInfo(id);
            if (state == null || info == null || !state.inStation()) return;
            info.spaceship.handleUserEvents(state.events, new HashMap<>());
        });
    }
}
