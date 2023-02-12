package ikuyo.server.renderers;

import ikuyo.api.Drawable;
import ikuyo.api.Star;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.Map;

public class UserRenderer implements DrawablesRenderer {
    @Override
    public void renderDrawables(Star star, Map<String, Drawable> drawables) {
        star.starInfo().starUsers.forEach((id, info) -> {
            if (!info.online) return;
            var text = new Drawable.Text();
            text.y = -70;
            text.text = "#%d(%.1f, %.1f)".formatted(id, info.x, info.y);
            text.style = JsonObject.of("fill", "red", "fontSize", 20);
            var pic = new Drawable.Sprite();
            pic.bundle = "other";
            pic.asset = "paimon";
            var drawable = new Drawable.Container();
            drawable.x = info.x;
            drawable.y = info.y;
            drawable.angle = Math.atan2(info.x, -info.y);
            drawable.zIndex = 1;
            drawable.children = new Drawable[] {pic, text};
            drawables.put("user#%d.position".formatted(id), drawable);
        });
    }
}
