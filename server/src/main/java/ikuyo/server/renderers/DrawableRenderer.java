package ikuyo.server.renderers;

import ikuyo.api.Drawable;
import ikuyo.api.Star;
import ikuyo.server.api.Renderer;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DrawableRenderer implements Renderer {
    @Override
    public JsonObject render(Star star) {
        var drawables = new ArrayList<Drawable>();
        for (var i = 0; i < star.starInfo().blocks.length; i++) {
            var block = star.starInfo().blocks[i];
            var d = new Drawable.Sprite();
            d.bundle = "blocks";
            d.asset = String.valueOf(block.type);
            drawables.add(d);
        }
        return new JsonObject(drawables.stream().map(JsonObject::mapFrom).collect(Collectors.toMap(
                json -> String.valueOf(json.hashCode()),
                Function.identity(),
                (s, a) -> s
        )));
    }
}
