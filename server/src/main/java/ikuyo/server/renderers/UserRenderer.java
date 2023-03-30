package ikuyo.server.renderers;

import ikuyo.api.Drawable;
import ikuyo.server.api.CommonContext;
import ikuyo.server.api.PhysicsEngine;
import org.dyn4j.geometry.Ray;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.result.RaycastResult;

import java.util.Map;

public class UserRenderer implements DrawablesRenderer {
    @Override
    public void renderDrawables(CommonContext ctx, Map<String, Drawable> drawables) {
        ctx.star().starInfo().starUsers.forEach((id, info) -> {
            drawPosition(ctx, drawables, id);
            drawCursor(ctx, drawables, id);
            drawGravityArrow(ctx, drawables, id);
        });
    }

    private static void drawPosition(CommonContext ctx, Map<String, Drawable> drawables, Integer id) {
        var info = ctx.star().starInfo().starUsers.get(id);
        if (info == null || !info.online) {
            drawables.put("user#%d.position".formatted(id), null);
            return;
        }
        var text = new Drawable.Text();
        text.y = -70;
        text.text = "%s(%.1f, %.1f)".formatted(ctx.users().get(id).name(), info.x, info.y);
        text.style = "{\"fill\":\"red\",\"fontSize\":20}";
        var pic = new Drawable.Sprite();
        pic.bundle = "other";
        pic.asset = "paimon";
        var drawable = new Drawable.Container();
        drawable.x = info.x * Drawable.scaling;
        drawable.y = info.y * Drawable.scaling;
        drawable.rotation = info.rotation;
        drawable.zIndex = 1;

        if (info.controlType.equals("fly")) {
            var ring = new Drawable.Sprite();
            ring.bundle = "other";
            ring.asset = "ring";
            ring.rotation = info.rotation;
            ring.zIndex = 1;
            drawable.children = new Drawable[] {pic, ring, text};
        } else
            drawable.children = new Drawable[] {pic, text};

        drawables.put("user#%d.position".formatted(id), drawable);
    }

    private static void drawCursor(CommonContext ctx, Map<String, Drawable> drawables, Integer id) {
        var info = ctx.star().starInfo().starUsers.get(id);
        if (info == null || !info.online) {
            drawables.put("user#%d.cursor".formatted(id), null);
            return;
        }
        var pos = ctx.userInputs().get(id).pointAt;
        var cursor = new Drawable.Sprite();
        cursor.x = pos.x * Drawable.scaling;
        cursor.y = pos.y * Drawable.scaling;
        cursor.bundle = "ui";
        cursor.asset = "greenCircle";
        cursor.zIndex = 2;
        cursor.user = id;
        drawables.put("user#%d.cursor".formatted(id), cursor);
    }

    private static void drawGravityArrow(CommonContext ctx, Map<String, Drawable> drawables, Integer id) {
        var info = ctx.star().starInfo().starUsers.get(id);
        if (info == null || !info.online || !"fly".equals(info.controlType)) {
            drawables.put("user#%d.gravityArrow".formatted(id), null);
            return;
        }

        var height = ctx.engine().rayCast(
                        new Ray(new Vector2(info.x, info.y), new Vector2(-info.x, -info.y)),
                        Math.hypot(info.x, info.y), filter -> filter.equals(PhysicsEngine.BLOCK))
                .stream().min(RaycastResult::compareTo).get().copy().getRaycast().getDistance();

        var pos = new Vector2(info.x, info.y);
        pos.multiply(pos.normalize() - Math.min(1 + height / 200, 2) * 5);
        var arrow = new Drawable.Sprite();
        arrow.x = pos.x * Drawable.scaling;
        arrow.y = pos.y * Drawable.scaling;
        arrow.rotation = -pos.getAngleBetween(Math.PI / 2);
        arrow.bundle = "ui";
        arrow.asset = "greenArrow";
        arrow.zIndex = 2;
        arrow.user = id;
        drawables.put("user#%d.gravityArrow".formatted(id), arrow);
    }
}
