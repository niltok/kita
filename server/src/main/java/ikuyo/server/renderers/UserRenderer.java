package ikuyo.server.renderers;

import ikuyo.api.Drawable;
import ikuyo.server.api.CommonContext;

import java.util.Map;

public class UserRenderer implements DrawablesRenderer {
    @Override
    public void renderDrawables(CommonContext ctx, Map<String, Drawable> drawables) {
        ctx.star().starInfo().starUsers.forEach((id, info) -> {
//            var info = ctx.common().star().starInfo().starUsers.get(id);
            if (info == null || !info.online) {
                drawables.put("user#%d.position".formatted(id), null);
                drawables.put("user#%d.cursor".formatted(id), null);
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
            drawable.rotation = Math.atan2(drawable.x, -drawable.y);
            drawable.zIndex = 1;
            drawable.children = new Drawable[] {pic, text};
            drawables.put("user#%d.position".formatted(id), drawable);
            drawCursor(ctx, drawables, id);
        });
    }

    private static void drawCursor(CommonContext ctx, Map<String, Drawable> drawables, Integer id) {
        var pos = ctx.userInputs().get(id).pointAt;
        var cursor = new Drawable.Sprite();
        cursor.x = pos.x * Drawable.scaling;
        cursor.y = pos.y * Drawable.scaling;
        cursor.bundle = "other";
        cursor.asset = "greenCircle";
        cursor.zIndex = 2;
        drawables.put("user#%d.cursor".formatted(id), cursor);
    }
}
