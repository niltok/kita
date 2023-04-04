package ikuyo.server.behaviors;

import ikuyo.api.behaviors.Behavior;
import ikuyo.server.api.CommonContext;
import org.dyn4j.geometry.Vector2;

import static ikuyo.api.Drawable.scaling;

public class PointerMovingBehavior implements Behavior<CommonContext> {
    @Override
    public void update(CommonContext context) {
        context.updated().users().forEach((id) -> {
            var state = context.getState(id);
            if (state == null) return;
            var input = state.input;
            var pointerMsgs = context.getState(id).events.get("star.operate.mouse");
            if (pointerMsgs != null && !pointerMsgs.isEmpty()) {
                var pointerMsg = pointerMsgs.get(pointerMsgs.size() - 1);
                var pos = input.relativePointer;
                if (pointerMsg.getDouble("x") != null && pointerMsg.getDouble("y") != null) {
                    pos.x = pointerMsg.getDouble("x") / scaling;
                    pos.y = pointerMsg.getDouble("y") / scaling;
                }
            }
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
