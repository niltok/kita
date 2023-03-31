package ikuyo.server.behaviors;

import ikuyo.api.behaviors.Behavior;
import ikuyo.server.api.CommonContext;

public class WeaponBehavior implements Behavior<CommonContext> {
    @Override
    public void update(CommonContext context) {
        context.star().starInfo().starUsers.forEach((id, info) -> {
            for (var weapon : info.spaceship.weapons) {
                weapon.frame();
            }
        });
    }
}
