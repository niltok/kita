package ikuyo.server.behaviors;

import ikuyo.api.behaviors.Behavior;
import ikuyo.server.api.CommonContext;
import org.dyn4j.geometry.Vector2;

public class PointerMovingBehavior implements Behavior<CommonContext> {
    @Override
    public void update(CommonContext context) {
        context.userInputs().forEach((id, input) -> {
            var camera = context.star().starInfo().starUsers.get(id);
            switch (camera.controlType) {
                case "fly" -> {
                    var pointer = new Vector2(input.relativePointer.x, input.relativePointer.y);
                    pointer.rotate(camera.rotation);
                    input.pointAt.x = pointer.x + camera.x;
                    input.pointAt.y = pointer.y + camera.y;
                }
                case "walk", default -> {
                    double dx = input.relativePointer.x, dy = input.relativePointer.y;
                    double cx = camera.x, cy = camera.y, cr = Math.hypot(cx, cy);
                    double nx = cx / cr, ny = cy / cr, nxy = nx * nx + ny * ny;
                    double x_ = (- nx * dy - ny * dx) / nxy, y_ = (nx * dx - ny * dy) / nxy;
                    input.pointAt.x = x_ + cx;
                    input.pointAt.y = y_ + cy;
                }
            }
        });
    }
}
