package ikuyo.server.behaviors;

import ikuyo.api.behaviors.Behavior;
import ikuyo.server.api.CommonContext;

public class WeaponBehavior implements Behavior<CommonContext> {
    @Override
    public void update(CommonContext context) {
        context.userStates().forEach((id, state) -> {
            var input = state.input;
            var info = context.getInfo(id);
            var ship = info.spaceship;
            if (input.prevWeapon > 0)
                ship.currentWeapon = (ship.currentWeapon - 1 + ship.weapons.length) % ship.weapons.length;
            if (input.nextWeapon > 0)
                ship.currentWeapon = (ship.currentWeapon + 1) % ship.weapons.length;
        });
    }
}
