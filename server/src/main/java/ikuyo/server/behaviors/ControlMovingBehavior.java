package ikuyo.server.behaviors;

public class ControlMovingBehavior extends AbstractBehavior {
    @Override
    public void update() {
        var speed = 5;
        context.userKeyInputs().forEach((id, input) -> {
            var pos = context.star().starInfo().starUsers.get(id);
            if (!pos.online) return;
            var z = Math.hypot(pos.x, pos.y);
            var dx = speed * (z == 0 ? 0 : pos.x / z);
            var dy = speed * (z == 0 ? -1 : pos.y / z);
            if (input.up > 0) {
                pos.x += dx;
                pos.y += dy;
            }
            if (input.down > 0) {
                pos.x -= dx;
                pos.y -= dy;
            }
            if (input.left > 0) {
                pos.x += dy;
                pos.y -= dx;
            }
            if (input.right > 0) {
                pos.x -= dy;
                pos.y += dx;
            }
        });
    }
}
