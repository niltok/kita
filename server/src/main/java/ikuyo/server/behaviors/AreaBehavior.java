package ikuyo.server.behaviors;

import ikuyo.api.behaviors.Behavior;
import ikuyo.api.datatypes.Drawable;
import ikuyo.server.api.CommonContext;
import ikuyo.utils.MsgDiffer;
import ikuyo.utils.StarUtils;

public class AreaBehavior implements Behavior<CommonContext> {
    @Override
    public void update(CommonContext context) {
        context.updated().users().forEach(id -> {
            var info = context.getInfo(id);
            if (info == null || !info.online) return;
            StarUtils.areasAround(info.x, info.y, MsgDiffer.cacheRange / Drawable.scaling).forEach(area -> {
                var state = context.areaStates.get(area);
                if (state.loaded) return;
                state.loaded = true;
                context.updated().blocks().addAll(StarUtils.getBlocksAt(area));
            });
        });
    }
}
