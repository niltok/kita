package ikuyo.server.behaviors;

import ikuyo.api.Drawable;
import ikuyo.api.StarInfo;
import ikuyo.api.behaviors.Behavior;
import ikuyo.server.api.BehaviorContext;

public class ControlMovingBehavior implements Behavior<BehaviorContext> {
    @Override
    public void update(BehaviorContext context) {
        var speed = 5;
        context.userKeyInputs().forEach((id, input) -> {
            var pos = context.star().starInfo().starUsers.get(id);
            if (!pos.online) return;
            var z = Math.hypot(pos.x, pos.y);
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
            }
            if (StarInfo.is_standable(
                    px / Drawable.scaling,
                    py / Drawable.scaling,
                    60 / Drawable.scaling,
                    context.star().starInfo())) {
                pos.x = px;
                pos.y = py;
            }
        });
    }
}
