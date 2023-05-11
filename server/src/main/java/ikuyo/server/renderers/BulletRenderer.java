package ikuyo.server.renderers;

import ikuyo.api.datatypes.Drawable;
import ikuyo.server.api.CommonContext;
import org.dyn4j.geometry.Vector2;

import java.util.ArrayList;
import java.util.Map;

public class BulletRenderer implements DrawablesRenderer {
    @Override
    public void renderDrawables(CommonContext ctx, Map<String, Drawable> drawables) {
        var removeList = new ArrayList<String>();

        ctx.dynamicEngine().bullets.forEach((id, bullet) ->{
            if (bullet == null) {
                drawables.put(id, null);
                removeList.add(id);
            }
            else {
                var newBullet = new Drawable.Sprite();
                Vector2 pos = bullet.body.getWorldCenter();
                newBullet.x = pos.x * Drawable.scaling;
                newBullet.y = pos.y * Drawable.scaling;
                newBullet.rotation = bullet.body.getTransform().getRotationAngle();
                newBullet.bundle = "bullet";
                newBullet.asset = bullet.type;
                newBullet.zIndex = 3;

                drawables.put(id, newBullet);
            }
        });

        for (var id : removeList)
            ctx.dynamicEngine().bullets.remove(id);
    }
}
