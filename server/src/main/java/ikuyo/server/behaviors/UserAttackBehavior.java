package ikuyo.server.behaviors;

import ikuyo.api.Position;
import ikuyo.api.behaviors.Behavior;
import ikuyo.server.api.Bullet;
import ikuyo.server.api.CommonContext;
import org.dyn4j.geometry.Vector2;

public class UserAttackBehavior implements Behavior<CommonContext> {

    @Override
    public void update(CommonContext context) {
        context.userInputs().forEach((id, input) -> {
            var userInfo = context.star().starInfo().starUsers.get(id);
            if (!userInfo.online) return;

            if (input.shot == 1) {
                Position point = input.pointAt;
                Vector2 userPos = new Vector2(context.star().starInfo().starUsers.get(id).x,
                        context.star().starInfo().starUsers.get(id).y);

                double radius = 0.4 + context.engine().users.get(id).getValue().getRotationDiscRadius();
                Vector2 direction = new Vector2(point.x - userPos.x, point.y - userPos.y);

//                todo: check position
                Bullet bullet = context.engine().addBullet(direction.getNormalized().multiply(radius).add(userPos));
                bullet.set(userInfo.weaponType, 5, 100);

//                v.add(context.engine().users.get(id).getValue().getLinearVelocity());
                bullet.body.setLinearVelocity(direction.getNormalized().multiply(150));
                bullet.body.setUserData(context.users().get(id).id());
            }
        });
    }
}
