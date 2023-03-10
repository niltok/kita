package ikuyo.server.renderers;

import ikuyo.api.Drawable;
import ikuyo.api.Star;
import ikuyo.server.api.RendererContext;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.Map;

public class UserRenderer implements DrawablesRenderer {
    @Override
    public void renderDrawables(RendererContext ctx, Map<String, Drawable> drawables) {
        ctx.common().star().starInfo().starUsers.forEach((id, info) -> {
//            var info = ctx.common().star().starInfo().starUsers.get(id);
            if (info == null || !info.online) {
                drawables.put("user#%d.position".formatted(id), null);
                return;
            }
            var text = new Drawable.Text();
            text.y = -70;
            text.text = "%s(%.1f, %.1f)".formatted(ctx.common().users().get(id).name(), info.x, info.y);
            text.style = "{\"fill\":\"red\",\"fontSize\":20}";
            var pic = new Drawable.Sprite();
            pic.bundle = "other";
            pic.asset = "paimon";
            var drawable = new Drawable.Container();
            drawable.x = info.x * Drawable.scaling;
            drawable.y = info.y * Drawable.scaling;
            drawable.angle = Math.atan2(drawable.x, -drawable.y);
            drawable.zIndex = 1;
            drawable.children = new Drawable[] {pic, text};
            drawables.put("user#%d.position".formatted(id), drawable);
        });
    }
}
