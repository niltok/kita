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
                Position userPos = new Position(context.star().starInfo().starUsers.get(id).x,
                        context.star().starInfo().starUsers.get(id).y);

                Bullet bullet = context.engine().addBullet(userInfo.weaponType, userPos);

                Vector2 v = new Vector2(point.x - userPos.x, point.y - userPos.y);
                v.normalize();
                v.multiply(300);
//                v.add(context.engine().users.get(id).getValue().getLinearVelocity());
                bullet.body.setLinearVelocity(v);
                bullet.body.setUserData(context.users().get(id).id());
            }
        });
    }
}
