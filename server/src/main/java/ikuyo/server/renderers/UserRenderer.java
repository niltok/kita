package ikuyo.server.renderers;

import com.google.common.collect.Sets;
import ikuyo.api.datatypes.Drawable;
import ikuyo.server.api.CommonContext;
import ikuyo.server.api.KitasBody;
import ikuyo.server.api.PhysicsEngine;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.AABB;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.result.DetectResult;

import java.util.Iterator;
import java.util.Map;

import static ikuyo.utils.StarUtils.areaSize;

public class UserRenderer implements DrawablesRenderer {
    @Override
    public void renderDrawables(CommonContext ctx, Map<String, Drawable> drawables) {
        Sets.union(ctx.getInfos().keySet(), ctx.updated().users())
            .forEach((id) -> drawUser(ctx, drawables, id));
    }

    private static void drawUser(CommonContext ctx, Map<String, Drawable> drawables, Integer id) {
        drawPosition(ctx, drawables, id);
        drawCursor(ctx, drawables, id);
        drawGravityArrow(ctx, drawables, id);
        drawEnemyArrow(ctx, drawables, id);
    }

    private static void drawPosition(CommonContext ctx, Map<String, Drawable> drawables, Integer id) {
        var info = ctx.star().starInfo().starUsers.get(id);
        if (info == null || !info.online) {
            drawables.put("user#%d.position".formatted(id), null);
            return;
        }
        var text = new Drawable.Text();
        text.y = -70;
        text.text = "%s(%.1f, %.1f)".formatted(ctx.getState(id).user.name(), info.x, info.y);
        text.style = "{\"fill\":\"red\",\"fontSize\":20}";
        var pic = new Drawable.Sprite();
        pic.bundle = "other";

        // TODO: 2023/6/27 spaceship type
        pic.asset = info.spaceship.type + "-" + info.controlType;
//        if (info.controlType.equals("fly")) pic.asset = "穿梭机";

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
        var pos = ctx.getState(id).input.pointAt;
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

        var height = ctx.engine().users.get(id).groundClearance;
        if (Double.isNaN(height)) {
            height = areaSize * 3;
        }

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

    private static void  drawEnemyArrow(CommonContext ctx, Map<String, Drawable> drawables, Integer id) {
        var info = ctx.star().starInfo().starUsers.get(id);
        if (info == null || !info.online) return;
        var userPos = new Vector2(info.x, info.y);
        Iterator<DetectResult<KitasBody, BodyFixture>> userIterator =
                ctx.engine().broadPhaseDetect(new AABB(userPos, 80),
                        filter -> filter.equals(PhysicsEngine.USER));
        while (userIterator.hasNext()) {
            var user = userIterator.next().getBody();
            var userid = (int)user.getUserData();
            if (userid != id) {
                var enemyPos = user.getWorldCenter();
                var pos = userPos.copy().add(enemyPos.copy().subtract(userPos).getNormalized().multiply(5));
                var arrow = new Drawable.Sprite();
                arrow.x = pos.x * Drawable.scaling;
                arrow.y = pos.y * Drawable.scaling;
                arrow.rotation = -enemyPos.copy().subtract(userPos).getAngleBetween(Math.PI * 2);
                arrow.bundle = "ui";
                arrow.asset = "redArrow";
                arrow.zIndex = 2;
                arrow.user = id;
                drawables.put("!user#%d.EnemyArrow-#%d".formatted(id, userid), arrow);
            }
        }
    }
}
