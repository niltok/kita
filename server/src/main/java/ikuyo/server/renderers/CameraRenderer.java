package ikuyo.server.renderers;

import ikuyo.api.renderers.Renderer;
import ikuyo.server.api.RendererContext;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;

public class CameraRenderer implements Renderer<RendererContext> {
    @Override
    public JsonObject render(RendererContext ctx) {
        var res = JsonObject.of();
        ctx.updated().users().forEach(id -> {
            var pos = ctx.star().starInfo().starUsers.get(id);
            if (pos == null) res.putNull(id.toString());
            else res.put(id.toString(), JsonObject.of("camera", JsonObject.of(
                    "x", pos.x,
                    "y", pos.y
            )));
        });
        return res;
    }
}
