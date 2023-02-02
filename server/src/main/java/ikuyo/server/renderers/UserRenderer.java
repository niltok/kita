package ikuyo.server.renderers;

import ikuyo.api.Drawable;
import ikuyo.api.Star;
import ikuyo.server.api.Renderer;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

public class UserRenderer implements Renderer {
    @Override
    public JsonObject render(Star star) {
        var drawables = new ArrayList<Drawable>();
        star.starInfo().starUsers.forEach((id, info) -> {
            if (!info.online) return;
            var drawable = new Drawable.Text();
            drawable.x = info.x;
            drawable.y = info.y;
            drawable.angle = Math.atan2(info.x, -info.y);
            drawable.text = "#%d(%.1f, %.1f)".formatted(id, info.x, info.y);
            drawable.style = JsonObject.of("fill", "red");
            drawables.add(drawable);
        });
        return new JsonObject(drawables.stream().map(JsonObject::mapFrom).collect(Collectors.toMap(
                json -> String.valueOf(json.hashCode()),
                Function.identity(),
                (s, a) -> s
        )));
    }
}
