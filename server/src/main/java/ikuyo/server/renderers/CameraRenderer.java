package ikuyo.server.renderers;

import ikuyo.api.datatypes.Drawable;
import ikuyo.api.renderers.Renderer;
import ikuyo.server.api.CommonContext;
import io.vertx.core.json.JsonObject;

public class CameraRenderer implements Renderer<CommonContext> {
    @Override
    public JsonObject render(CommonContext ctx) {
        var res = JsonObject.of();
        ctx.updated().users().forEach(id -> {
            var info = ctx.star().starInfo().starUsers.get(id);
            if (info == null || !info.online) res.putNull(id.toString());
            else {
                JsonObject object = JsonObject.of(
                        "x", info.cameraX * Drawable.scaling,
                        "y", info.cameraY * Drawable.scaling,
                        "rotation", switch (info.controlType) {
                            case "fly" -> info.rotation;
                            case "walk", default -> Math.atan2(info.x, -info.y);
                        },
                        "rebirth", "rebirth".equals(info.controlType)
                );
                res.put(id.toString(), object);
            }
        });
        return res;
    }

}
