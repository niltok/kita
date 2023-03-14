package ikuyo.server.behaviors;

import ikuyo.api.Drawable;
import ikuyo.api.Position;
import ikuyo.api.StarInfo;
import ikuyo.api.behaviors.Behavior;
import ikuyo.server.api.BehaviorContext;
import ikuyo.server.api.PhysicsEngine;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.Force;
import org.dyn4j.geometry.Vector2;

public class ControlMovingBehavior implements Behavior<BehaviorContext> {
    /**单一方向上施加力的速度上限*/
    private static final double speed = 3000;
    /**单一方向上施加的力的最大值*/
    private static final double maxForce = 100000;
    /**单位向量，计算用*/
    private static final Vector2 i = new Vector2(1, 0);
    @Override
    public void update(BehaviorContext context) {
        context.userKeyInputs().forEach((id, input) -> {
            var pos = context.common().star().starInfo().starUsers.get(id);
            if (!pos.online) return;

            double angle = (Math.atan2(pos.y, pos.x) + Math.PI * 2) % (Math.PI * 2);
            Vector2 force = new Vector2();
            Body body = context.common().engine().users
                    .get(id).getValue();

            if (context.common().users().get(id).isAdmin()) {
                if (input.up > 0) {
                    force.add(new Vector2(i));
                }
                if (input.down > 0) {
                    force.add(new Vector2(i).rotate(Math.PI));
                }
            }

            if (input.jump > 0) {
                body.setLinearVelocity(body.getLinearVelocity()
                        .add(new Vector2(speed, 0).rotate(angle)));
            }
            if (input.left > 0) {
                force.add(new Vector2(i).rotate(-Math.PI / 2));
            }
            if (input.right > 0) {
                force.add(new Vector2(i).rotate(Math.PI / 2));
            }
            if (!force.equals(0.0, 0.0)) {
                force.rotate(angle);
                force.normalize();
                force.multiply(maxForce *
                        Math.max(Math.pow(1 - Math.max(body.getLinearVelocity().dot(force), 0) / speed, 5), 0));
                body.applyForce(force);
//                System.out.println("{ControlMoving} [x]: %f, [y]: %f".formatted(force.x, force.y));
            }
        });
    }
}
