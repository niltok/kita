package ikuyo.server.behaviors;

import ikuyo.api.UserInput;
import ikuyo.api.behaviors.Behavior;
import ikuyo.server.api.CommonContext;
import ikuyo.server.api.KitasBody;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.result.DetectResult;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;

public class ControlMovingBehavior implements Behavior<CommonContext> {
    /**单一方向上施加力的速度上限*/
    private static final double speed = 100;
    /**单一方向上施加的加速度的最大值*/
    private static final double maxAcc = 500;
    /**单位向量，计算用*/
    private static final Vector2 i = new Vector2(1, 0);
    @Override
    public void update(CommonContext context) {
        context.updated().users().forEach((id) -> {
            var state = context.getState(id);
            var userInfo = context.getInfo(id);
            if (state == null || userInfo == null || !userInfo.online) return;
            var input = state.input;
            var body = context.engine().users.get(id).getValue();

            if (userInfo.controlType.equals("fly")) {
                Iterator<DetectResult<KitasBody, BodyFixture>> iterator =
                        context.engine().broadPhaseDetect(body, null);
                if (iterator.hasNext() && context.engine().ManifoldDetect(body, iterator)) {
                    userInfo.controlType = "walk";
                    body.setGravityScale(1);
                    body.setFixRotation(true);
                }
            }

            if (!userInfo.controlType.equals("destroyed")) {
                switch (input.jumpOrFly) {
                    case 2 -> {
                        if (input.flyWhen.isBefore(Instant.now())) {
                            userInfo.controlType = "fly";
                            body.setAngularVelocity(0);
                            body.setGravityScale(0.05);
                            body.setFixRotation(false);
                        }
                    }
                    case 3 -> input.flyWhen = Instant.now().plus(Duration.ofMillis(500));
                }
            }

            if (context.getState(id).user.isAdmin()) {
                userInfo.controlType = "fly";
                body.setGravityScale(0);
                body.setFixRotation(false);
            }

            controlMoving(body, userInfo.controlType, input);
        });
    }

    private void controlMoving(KitasBody body, String type, UserInput input) {
        double angle = (Math.atan2(body.getWorldCenter().y, body.getWorldCenter().x) + Math.PI * 2) % (Math.PI * 2);
        Vector2 force = new Vector2();
        switch (type) {
            case "walk", default -> {
                if (input.jumpOrFly == 3) {
                    body.setLinearVelocity(body.getLinearVelocity()
                            .add(new Vector2(speed / 1.5, 0).rotate(angle)));
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
                    force.multiply(maxAcc * body.getMass().getMass() *
                            Math.max(Math.pow(1 - Math.max(body.getLinearVelocity().dot(force), 0) / speed, 5), 0));
                    body.applyForce(force);
                }
            }
            case "fly" -> {
                double rotationAngle = body.getTransform().getRotationAngle();
                if (input.up > 0 || input.jumpOrFly > 0) {
                    force.add(new Vector2(i));
                }
                if (input.down > 0) {
                    force.add(new Vector2(i).rotate(Math.PI));
                }
                if (input.left > 0) {
                    body.applyImpulse(new Vector2(10, 0).rotate(rotationAngle),
                            new Vector2(body.getWorldCenter()).add(2, 2).rotate(rotationAngle));
                    body.applyImpulse(new Vector2(-10, 0).rotate(rotationAngle),
                            new Vector2(body.getWorldCenter()).add(-2, -2).rotate(rotationAngle));
                    body.setEnabled(true);
                }
                if (input.right > 0) {
                    body.applyImpulse(new Vector2(10, 0).rotate(rotationAngle),
                            new Vector2(body.getWorldCenter()).add(2, -2).rotate(rotationAngle));
                    body.applyImpulse(new Vector2(-10, 0).rotate(rotationAngle),
                            new Vector2(body.getWorldCenter()).add(-2, 2).rotate(rotationAngle));
                    body.setEnabled(true);
                }
                if (!force.equals(0.0, 0.0)) {
                    force.rotate(rotationAngle);
                    force.normalize();
                    force.multiply(maxAcc * body.getMass().getMass() *
                            Math.max(Math.pow(1 - Math.max(body.getLinearVelocity().dot(force), 0) / (speed * 10), 5), 0));
                    body.applyForce(force);
                }
            }
            case "destroyed" -> {

            }
        }
    }
}
