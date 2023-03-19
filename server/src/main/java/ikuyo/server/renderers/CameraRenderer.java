package ikuyo.server.renderers;

import ikuyo.api.Drawable;
import ikuyo.api.renderers.Renderer;
import ikuyo.server.api.CommonContext;
import io.vertx.core.json.JsonObject;

public class CameraRenderer implements Renderer<CommonContext> {
    @Override
    public JsonObject render(CommonContext ctx) {
        var res = JsonObject.of();
        ctx.updated().users().forEach(id -> {
            var pos = ctx.star().starInfo().starUsers.get(id);
            if (pos == null) res.putNull(id.toString());
            else res.put(id.toString(), JsonObject.of("camera", JsonObject.of(
                    "x", pos.x * Drawable.scaling,
                    "y", pos.y * Drawable.scaling,
                    "rotation", -Math.atan2(pos.x, -pos.y)
            )));
        });
        return res;
    }
}
