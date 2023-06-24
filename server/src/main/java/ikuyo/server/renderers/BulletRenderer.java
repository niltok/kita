package ikuyo.server.renderers;

import ikuyo.api.datatypes.Drawable;
import ikuyo.server.api.CommonContext;

import java.util.ArrayList;
import java.util.Map;

public class BulletRenderer implements DrawablesRenderer {
    @Override
    public void renderDrawables(CommonContext ctx, Map<String, Drawable> drawables) {
        var removeList = new ArrayList<String>();

        ctx.engine().bullets.forEach((id, bullet) ->{
            if (bullet == null) {
                drawables.put(id, null);
                removeList.add(id);
            }
            else {
                // TODO: 2023/6/23 update
                bullet.updateDrawable();
                if (bullet.getDrawable() != null) {
                    Drawable newBullet = bullet.getDrawable().clone();
                    drawables.put(id, newBullet);
                }
                else {
                    drawables.put(id, null);
                    removeList.add(id);
                }
            }
        });

        for (var id : removeList)
            ctx.engine().bullets.remove(id);
    }
}
