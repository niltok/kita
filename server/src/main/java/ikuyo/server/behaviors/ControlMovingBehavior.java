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
//        var speed = 5;
        context.userKeyInputs().forEach((id, input) -> {
            var pos = context.common().star().starInfo().starUsers.get(id);
            if (!pos.online) return;
            /*var z = Math.hypot(pos.x, pos.y);
            var dx = speed * (z == 0 ? 0 : pos.x / z);
            var dy = speed * (z == 0 ? -1 : pos.y / z);
            var px = pos.x;
            var py = pos.y;
            if (input.up > 0) {
                px += dx;
                py += dy;
            }
            if (input.down > 0) {
                px -= dx;
                py -= dy;
            }
            if (input.left > 0) {
                px += dy;
                py -= dx;
            }
            if (input.right > 0) {
                px -= dy;
                py += dx;
            }*/
            double angle = (Math.atan2(pos.x, pos.y) + Math.PI * 2) % (Math.PI * 2);
            Vector2 speed = new Vector2(0, 200);
            Vector2 force = new Vector2(0, 0);
            if (input.up > 0) {
                force = speed;
            }
            if (input.down > 0) {
                force = force.add(speed.rotate(Math.PI));
            }
            if (input.left > 0) {
                force = force.add(speed.rotate(-Math.PI / 2));
            }
            if (input.right > 0) {
                force = force.add(speed.rotate(Math.PI / 2));
            }
            Body body = context.common().engine().users.get(id);
//            System.out.println("{ControlMoving} [x]: %f, [y]: %f".formatted(body.getWorldCenter().x, body.getWorldCenter().y));
            body.setLinearVelocity(force.rotate(-angle));
            /*if (context.common().users().get(id).isAdmin() || StarInfo.is_standable(
                    px / Drawable.scaling,
                    py / Drawable.scaling,
                    60 / Drawable.scaling,
                    context.common().star().starInfo())) {
                if (pos.x != px || pos.y != py) context.common().updated().users().add(id);
                pos.x = px;
                pos.y = py;
            }*/
        });
    }
}
