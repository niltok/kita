package ikuyo.server.behaviors;

import ikuyo.api.behaviors.Behavior;
import ikuyo.server.api.CommonContext;

public class PointerMovingBehavior implements Behavior<CommonContext> {
    @Override
    public void update(CommonContext context) {
        context.userInputs().forEach((id, input) -> {
            double dx = input.relativePointer.x, dy = input.relativePointer.y;
            var camera = context.star().starInfo().starUsers.get(id);
            double cx = camera.x, cy = camera.y, cr = Math.hypot(cx, cy);
            double nx = cx / cr, ny = cy / cr, nxy = nx * nx + ny * ny;
            double x_ = (- nx * dy - ny * dx) / nxy, y_ = (nx * dx - ny * dy) / nxy;
            input.pointAt.x = x_ + cx;
            input.pointAt.y = y_ + cy;
        });
    }
}
