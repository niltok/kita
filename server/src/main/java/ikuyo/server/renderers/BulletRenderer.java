package ikuyo.server.renderers;

import ikuyo.api.Drawable;
import ikuyo.server.api.CommonContext;
import org.dyn4j.geometry.Vector2;

import java.util.Map;

public class BulletRenderer implements DrawablesRenderer {
    @Override
    public void renderDrawables(CommonContext ctx, Map<String, Drawable> drawables) {
        ctx.engine().bullets.forEach((id, bullet) ->{
            if (bullet == null)
                drawables.put(id, null);
            else {
                var newBullet = new Drawable.Sprite();
                Vector2 pos = bullet.body.getWorldCenter();
                newBullet.x = pos.x * Drawable.scaling;
                newBullet.y = pos.y * Drawable.scaling;
                newBullet.bundle = "bullet";
                newBullet.asset = bullet.type;
                newBullet.zIndex = 3;

                drawables.put(id, newBullet);
            }
        });
    }
}
