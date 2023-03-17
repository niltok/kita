package ikuyo.server.renderers;

import ikuyo.api.Drawable;
import ikuyo.api.User;
import ikuyo.server.api.Bullet;
import ikuyo.server.api.CommonContext;
import org.dyn4j.geometry.Vector2;

import java.util.ArrayList;
import java.util.Map;

public class BulletRenderer implements DrawablesRenderer {
    @Override
    public void renderDrawables(CommonContext ctx, Map<String, Drawable> drawables) {
        var remove = new ArrayList<String>();
        for (Map.Entry<String, Bullet> entry : ctx.engine().bullets.entrySet()) {
            String id = entry.getKey();
            Bullet bullet = entry.getValue();
            if (bullet == null)
                drawables.put(id, null);
            else {
                ctx.engine().updateBullet(bullet);
                if ( bullet.colligionIterator.hasNext()
                        && ctx.engine().bulletCheck(bullet) ) {
//                    todo: remove blocks
                    remove.add(id);
                } else {
//                    User user = ctx.users().get((int)bullet.body.getUserData());
//                    int weaponType = 1;
////                    todo:check state
//                    if (ctx.star().starInfo().starUsers.containsKey(user.id()))
//                        weaponType = ctx.star().starInfo().starUsers.get(user.id()).weaponType;
                    var newBullet = new Drawable.Sprite();
                    Vector2 pos = bullet.body.getWorldCenter();
                    newBullet.x = pos.x * Drawable.scaling;
                    newBullet.y = pos.y * Drawable.scaling;
                    newBullet.bundle = "bullet";
                    newBullet.asset = String.format("%d", bullet.type);
                    newBullet.zIndex = 3;

                    drawables.put(id, newBullet);
                }
            }
        }
        for (var id : remove) {
            ctx.engine().removeBullet(id);
            drawables.put(id, null);
        }
    }
}
