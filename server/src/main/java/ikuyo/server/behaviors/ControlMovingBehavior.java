package ikuyo.server.behaviors;

import ikuyo.api.Drawable;
import ikuyo.api.Position;
import ikuyo.api.StarInfo;
import ikuyo.api.behaviors.Behavior;
import ikuyo.server.api.BehaviorContext;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.Force;
import org.dyn4j.geometry.Vector2;

public class ControlMovingBehavior implements Behavior<BehaviorContext> {
    @Override
    public void update(BehaviorContext context) {
        context.userKeyInputs().forEach((id, input) -> {
            var pos = context.common().star().starInfo().starUsers.get(id);
            if (!pos.online) return;

            double angle = (Math.atan2(pos.y, pos.x) + Math.PI * 2) % (Math.PI * 2);
            final Vector2 speed = new Vector2(1000, 0);
            Vector2 force = new Vector2();
            if (input.up > 0) {
                force.add(speed);
            }
            if (input.down > 0) {
                force.add(speed.rotate(Math.PI));
            }
            if (input.left > 0) {
                force.add(speed.rotate(-Math.PI / 2));
            }
            if (input.right > 0) {
                force.add(speed.rotate(Math.PI / 2));
            }
            if (!force.equals(0.0, 0.0)) {
                Body body = context.common().engine().users
                        .get(id).getValue();
                force.rotate(angle);
                body.applyForce(force);
//                System.out.println("{ControlMoving} [x]: %f, [y]: %f".formatted(body.getWorldCenter().x, body.getWorldCenter().y));
            }
        });
    }
}
