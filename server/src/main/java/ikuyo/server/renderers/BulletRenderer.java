package ikuyo.server.renderers;

import ikuyo.api.Drawable;
import ikuyo.api.User;
import ikuyo.server.api.CommonContext;
import org.dyn4j.geometry.Vector2;

import java.util.Map;

public class BulletRenderer implements DrawablesRenderer {
    @Override
    public void renderDrawables(CommonContext ctx, Map<String, Drawable> drawables) {
        ctx.engine().bullets.forEach((id, body) -> {

            if (body == null)
                drawables.put(id, null);
            else {
                User user = ctx.users().get((int)body.getUserData());
                int weaponType = 1;
                if (ctx.star().starInfo().starUsers.containsKey(user.id()))
                    weaponType = ctx.star().starInfo().starUsers.get(user.id()).weaponType;
                var bullet = new Drawable.Sprite();
                Vector2 pos = body.getWorldCenter();
                bullet.x = pos.x * Drawable.scaling;
                bullet.y = pos.y * Drawable.scaling;
                bullet.bundle = "bullet";
                bullet.asset = String.format("%d", weaponType);
                bullet.zIndex = 3;

                drawables.put(id, bullet);
            }
        });
    }
}
