package ikuyo.server.renderers;

import ikuyo.api.renderers.Renderer;
import ikuyo.server.api.RendererContext;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;

public class CameraRenderer implements Renderer<RendererContext> {
    @Override
    public JsonObject render(RendererContext ctx) {
        var star = ctx.star();
        var map = new HashMap<String, Object>();
        star.starInfo().starUsers.forEach((id, pos) -> {
            map.put(id.toString(), JsonObject.of("camera", JsonObject.of(
                    "x", 0,
                    "y", Math.hypot(pos.x, pos.y),
                    "rotation", -Math.atan2(pos.x, -pos.y)
            )));
        });
        return new JsonObject(map);
    }
}
