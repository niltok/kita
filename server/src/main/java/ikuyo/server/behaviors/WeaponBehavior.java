package ikuyo.server.behaviors;

import ikuyo.api.behaviors.Behavior;
import ikuyo.server.api.CommonContext;

public class WeaponBehavior implements Behavior<CommonContext> {
    @Override
    public void update(CommonContext context) {
        context.updated().users().forEach((id) -> {
            var state = context.getState(id);
            if (state == null) return;
            var input = state.input;
            var info = context.getInfo(id);
            var ship = info.spaceship;
            if (input.prevWeapon > 0)
                ship.currentWeapon = (ship.currentWeapon - 1 + ship.weapons.size()) % ship.weapons.size();
            if (input.nextWeapon > 0)
                ship.currentWeapon = (ship.currentWeapon + 1) % ship.weapons.size();
        });
    }
}
