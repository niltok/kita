package ikuyo.server.renderers;

import ikuyo.api.Drawable;
import ikuyo.server.api.CommonContext;

import java.util.Map;

public class UserRenderer implements DrawablesRenderer {
    @Override
    public void renderDrawables(CommonContext ctx, Map<String, Drawable> drawables) {
        ctx.star().starInfo().starUsers.forEach((id, info) -> {
            drawPosition(ctx, drawables, id);
            drawCursor(ctx, drawables, id);
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
        drawable.rotation = info.rotation + Math.PI / 2;
        drawable.zIndex = 1;
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
        cursor.bundle = "other";
        cursor.asset = "greenCircle";
        cursor.zIndex = 2;
        cursor.user = id;
        drawables.put("user#%d.cursor".formatted(id), cursor);
    }
}
