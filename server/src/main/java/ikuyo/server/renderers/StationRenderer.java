package ikuyo.server.renderers;

import ikuyo.api.datatypes.Drawable;
import ikuyo.api.datatypes.Station;
import ikuyo.server.api.CommonContext;
import ikuyo.utils.Position;
import io.vertx.core.buffer.Buffer;

import java.util.List;
import java.util.Map;

public class StationRenderer implements DrawablesRenderer {
    @Override
    public void renderDrawables(CommonContext context, Map<String, Drawable> drawables) {
        if (!context.updated().init()) return;
        List<Station> stations = context.star().stations();
        for (int i = 0, stationsSize = stations.size(); i < stationsSize; i++) {
            var s = stations.get(i);
            var d = new Drawable.Sprite();
            Position pos = s.pos;
            d.x = pos.x * Drawable.scaling;
            d.y = pos.y * Drawable.scaling;
            d.rotation = Math.atan2(pos.x, -pos.y);
            d.bundle = "ui";
            d.asset = "station";
            drawables.put(Buffer.buffer()
                    .appendString("station#")
                    .appendString(String.valueOf(i))
                    .appendString(".image").toString(), d);
        }
    }
}
