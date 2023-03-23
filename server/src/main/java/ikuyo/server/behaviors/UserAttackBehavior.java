package ikuyo.server.behaviors;

import ikuyo.api.Position;
import ikuyo.api.behaviors.Behavior;
import ikuyo.server.api.Bullet;
import ikuyo.server.api.CommonContext;
import ikuyo.server.api.KitasBody;
import ikuyo.server.api.PhysicsEngine;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.Ray;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.result.RaycastResult;

import java.util.List;

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

                double radius = 0.3 + context.engine().users.get(id).getValue().getRotationDiscRadius();
                Vector2 direction = new Vector2(point.x - userPos.x, point.y - userPos.y).getNormalized();
                var bulletPos = direction.copy().multiply(radius).add(userPos);
                var bulletVelocity = direction.copy().multiply(150);

                List<RaycastResult<KitasBody, BodyFixture>> result =
                        context.engine().rayCast(new Ray(userPos, direction),
                                radius + 0.3, filter -> filter.equals(PhysicsEngine.BLOCK));
                if (result.size() != 0) {
                    bulletPos = result.get(0).copy().getRaycast().getPoint()
                            .subtract(direction.copy().multiply(0.3));
                    bulletVelocity = new Vector2();
                }

                Bullet bullet = context.engine().addBullet(bulletPos);
                bullet.set(userInfo.weaponType, 5, 100);
//                v.add(context.engine().users.get(id).getValue().getLinearVelocity());
                bullet.body.setLinearVelocity(bulletVelocity);
                bullet.body.setUserData(context.users().get(id).id());
            }
        });
    }
}
