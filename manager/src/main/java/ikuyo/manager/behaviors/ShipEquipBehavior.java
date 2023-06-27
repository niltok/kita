package ikuyo.manager.behaviors;

import ikuyo.api.behaviors.Behavior;
import ikuyo.api.entities.StationCargo;
import ikuyo.manager.api.CommonContext;

import java.util.Map;

public class ShipEquipBehavior implements Behavior<CommonContext> {
    @Override
    public void update(CommonContext context) {
        context.updated().users().forEach(id -> {
            var state = context.getState(id);
            var info = context.getInfo(id);
            if (state == null || info == null || !state.inStation()) return;
            var modify = info.spaceship.handleUserEvents(state.events,
                    Map.of("空间站", state.stationCargo), "空间站");
            if (modify) StationCargo.put(context.sql(), state.user, state.stationCargo);
        });
    }
}
