package ikuyo.server.behaviors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ikuyo.api.behaviors.Behavior;
import ikuyo.api.datatypes.Drawable;
import ikuyo.server.api.AreaState;
import ikuyo.server.api.CommonContext;
import ikuyo.utils.MsgDiffer;
import ikuyo.utils.StarUtils;

import java.util.HashSet;
import java.util.List;

public class AreaBehavior implements Behavior<CommonContext> {
    @Override
    public void update(CommonContext context) {
        var mapper = new ObjectMapper();
        var updated = context.updated();
        if (updated.init()) {
            var states = context.areaStates;
            for (int i = 0; i < states.size(); i++) {
                var state = states.get(i);
                var blocks = StarUtils.getBlocksAt(i);
                writeCache(context, mapper, state, blocks);
            }
        }
        updated.users().forEach(id -> {
            var info = context.getInfo(id);
            if (info == null || !info.online) return;
            StarUtils.areasAround(info.x, info.y, MsgDiffer.cacheRange / Drawable.scaling).forEach(area -> {
                var state = context.areaStates.get(area);
                if (state.loaded) return;
                state.loaded = true;
                var blocks = StarUtils.getBlocksAt(area);
                updated.blocks().addAll(blocks);
            });
        });
        if (context.writeBackLock.get()) return;
        updated.areas.forEach(area -> {
            writeCache(context, mapper, context.areaStates.get(area), StarUtils.getBlocksAt(area));
        });
        updated.areas = new HashSet<>();
    }

    private static void writeCache(CommonContext context, ObjectMapper mapper, AreaState state, List<Integer> blocks) {
        blocks.forEach(i -> {
            try {
                state.cached.put(i, mapper.writeValueAsString(context.star().starInfo().blocks[i]));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        });
    }
}
