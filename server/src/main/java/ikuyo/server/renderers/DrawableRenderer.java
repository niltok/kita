package ikuyo.server.renderers;

import ikuyo.api.Drawable;
import ikuyo.api.Position;
import ikuyo.api.Star;
import ikuyo.api.StarInfo;
import ikuyo.server.api.Renderer;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DrawableRenderer implements Renderer {
    @Override
    public JsonObject render(Star star) {
        var drawables = new ArrayList<Drawable>();
        int rindex = StarInfo.realINdexOf(0, star.starInfo().mintier);
        for (var i = 0; i < star.starInfo().blocks.length; i++) {
            var block = star.starInfo().blocks[i];
            var d = new Drawable.Sprite();
            d.key = "block#%d.image".formatted(i);
            Position pos = StarInfo.posOf(rindex);
            d.x = pos.x;
            d.y = pos.y;
            d.bundle = "blocks";
            d.asset = String.valueOf(block.type);
            drawables.add(d);
            rindex++;
        }
        return new JsonObject(drawables.stream().map(JsonObject::mapFrom).collect(Collectors.toMap(
                json -> {
                    var key = json.getString("key");
                    json.putNull("key");
                    var hash = String.valueOf(json.hashCode());
                    json.put("key", key);
                    return hash;
                },
                Function.identity(),
                (s, a) -> s
        )));
    }
}
