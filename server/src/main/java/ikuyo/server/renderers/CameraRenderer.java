package ikuyo.server.renderers;

import ikuyo.api.Star;
import ikuyo.server.api.Renderer;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;

public class CameraRenderer implements Renderer {
    @Override
    public JsonObject render(Star star) {
        var map = new HashMap<String, Object>();
        star.starInfo().starUsers.forEach((id, pos) -> {
            map.put(id.toString(), JsonObject.of("camera", JsonObject.of(
                    "x", pos.x,
                    "y", pos.y,
                    "rotation", 0 //Math.atan2(pos.y, pos.x)
            )));
        });
        return new JsonObject(map);
    }
}
