package ikuyo.server.behaviors;

import ikuyo.api.UserInput;
import ikuyo.api.behaviors.Behavior;
import ikuyo.server.api.CommonContext;
import org.dyn4j.dynamics.Body;
import org.dyn4j.geometry.Vector2;

import java.time.Duration;
import java.time.Instant;

public class ControlMovingBehavior implements Behavior<CommonContext> {
    /**单一方向上施加力的速度上限*/
    private static final double speed = 100;
    /**单一方向上施加的加速度的最大值*/
    private static final double maxAcc = 1000;
    /**单位向量，计算用*/
    private static final Vector2 i = new Vector2(1, 0);
    @Override
    public void update(CommonContext context) {
        context.userInputs().forEach((id, input) -> {
            var userInfo = context.star().starInfo().starUsers.get(id);
            if (!userInfo.online) return;

            switch (input.jumpOrFly) {
                case 1 -> {
                    userInfo.controlType = "walk";
                    context.engine().users.get(id).getValue().setBearTheGravity(true);
                }
                case 2 -> {
                    if (input.flyWhen.isBefore(Instant.now())) {
                        userInfo.controlType = "fly";
                        var body = context.engine().users.get(id).getValue();
                        body.setBearTheGravity(false);
                    }
                }
                case 3 -> input.flyWhen = Instant.now().plus(Duration.ofSeconds(1));
            }

            if (context.users().get(id).isAdmin()) {
                userInfo.controlType = "fly";
            }

            movingControl(context.engine().users.get(id).getValue(), userInfo.controlType, input);
        });
    }

    private void movingControl(Body body, String type, UserInput input) {
        double angle = (Math.atan2(body.getWorldCenter().y, body.getWorldCenter().x) + Math.PI * 2) % (Math.PI * 2);
        Vector2 force = new Vector2();
        switch (type) {
            case "walk", default -> {
                if (input.jumpOrFly == 3) {
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
                    force.multiply(maxAcc * body.getMass().getMass() *
                            Math.max(Math.pow(1 - Math.max(body.getLinearVelocity().dot(force), 0) / speed, 5), 0));
                    body.applyForce(force);
                }
            }
            case "fly" -> {
                if (input.up > 0 || input.jumpOrFly > 0) {
                    force.add(new Vector2(i));
                }
                if (input.down > 0) {
                    force.add(new Vector2(i).rotate(Math.PI));
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
        }
    }
}
