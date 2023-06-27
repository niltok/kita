package ikuyo.manager.behaviors;

import ikuyo.api.behaviors.Behavior;
import ikuyo.api.entities.StationCargo;
import ikuyo.manager.api.CommonContext;

public class CargoBehavior implements Behavior<CommonContext> {
    @Override
    public void update(CommonContext context) {
        context.updated().users().forEach(id -> {
            var state = context.getState(id);
            var info = context.getInfo(id);
            if (state == null || info == null || !state.inStation()) return;
            var events = state.events;
            var modify = false;
            var loadMsg = events.get("ship.load");
            var cargo = info.spaceship.cargoHold;
            if (loadMsg != null) for (var msg : loadMsg) {
                if (msg.containsKey("key")) {
                    var key = msg.getString("key");
                    if (cargo.put(key) == 0) {
                        modify = true;
                        state.stationCargo.take(key);
                    }
                } else {
                    var index = msg.getInteger("index");
                    var unpack = cargo.unpacks.get(index);
                    if (cargo.put(unpack) == 0) {
                        modify = true;
                        state.stationCargo.take(index);
                    }
                }
            }
            var unloadMsg = events.get("ship.unload");
            if (unloadMsg != null) for (var msg : unloadMsg) {
                if (msg.containsKey("key")) {
                    var key = msg.getString("key");
                    if (state.stationCargo.put(key) == 0) {
                        modify = true;
                        cargo.take(key);
                    }
                } else {
                    var index = msg.getInteger("index");
                    var unpack = cargo.unpacks.get(index);
                    if (state.stationCargo.put(unpack) == 0) {
                        modify = true;
                        cargo.take(index);
                    }
                }
            }
            if (modify) StationCargo.put(context.sql(), state.user, state.stationCargo);
        });
    }
}
