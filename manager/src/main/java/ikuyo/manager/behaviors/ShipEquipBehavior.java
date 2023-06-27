package ikuyo.manager.behaviors;

import ikuyo.api.behaviors.Behavior;
import ikuyo.api.cargo.CargoHold;
import ikuyo.api.entities.StationCargo;
import ikuyo.api.spaceships.Spaceship;
import ikuyo.api.spaceships.SpaceshipItem;
import ikuyo.manager.api.CommonContext;

import java.util.Map;

public class ShipEquipBehavior implements Behavior<CommonContext> {
    @Override
    public void update(CommonContext context) {
        context.updated().users().forEach(id -> {
            var state = context.getState(id);
            var info = context.getInfo(id);
            if (state == null || info == null || !state.inStation()) return;
            var switchMsg = state.events.get("ship.switch");
            var modify = false;
            CargoHold cargo = state.stationCargo;
            if (switchMsg != null && !switchMsg.isEmpty()) {
                var msg = switchMsg.get(switchMsg.size() - 1);
                var ship = info.spaceship;
                if (msg.containsKey("key")) {
                    var key = msg.getString("key");
                    SpaceshipItem item = SpaceshipItem.get(key);
                    if (item != null && cargo.take(key) == 0 && cargo.put(ship) == 0) {
                        var next = item.unpack();
                        ship.undeploy();
                        next.deploy(info);
                        modify = true;
                    }
                } else {
                    var index = msg.getInteger("index");
                    var unpack = cargo.unpacks.get(index);
                    if (unpack instanceof Spaceship next && cargo.take(index) == 0
                            && cargo.put(ship) == 0) {
                        next.deploy(info);
                        ship.undeploy();
                        modify = true;
                    }
                }
            }
            modify |= info.spaceship.handleUserEvents(state.events,
                    Map.of("空间站", cargo), "空间站");
            if (modify) StationCargo.put(context.sql(), state.user, cargo);
        });
    }
}
