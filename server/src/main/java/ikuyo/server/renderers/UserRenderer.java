package ikuyo.server.renderers;

import ikuyo.api.Drawable;
import ikuyo.api.Star;
import io.vertx.core.json.JsonObject;

import java.util.Map;

public class UserRenderer implements DrawablesRenderer {
    @Override
    public void renderDrawables(Star star, Map<String, Drawable> drawables) {
        star.starInfo().starUsers.forEach((id, info) -> {
            if (!info.online) return;
            var drawable = new Drawable.Text();
            drawable.x = info.x;
            drawable.y = info.y;
            drawable.angle = Math.atan2(info.x, -info.y);
            drawable.zIndex = 1;
            drawable.text = "#%d(%.1f, %.1f)".formatted(id, info.x, info.y);
            drawable.style = JsonObject.of("fill", "red");
            drawables.put("user#%d.position".formatted(id), drawable);
        });
    }
}
