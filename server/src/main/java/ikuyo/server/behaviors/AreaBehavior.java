package ikuyo.server.behaviors;

import com.fasterxml.jackson.core.JsonProcessingException;
import ikuyo.api.behaviors.Behavior;
import ikuyo.api.datatypes.Drawable;
import ikuyo.server.api.AreaState;
import ikuyo.server.api.CommonContext;
import ikuyo.utils.DataStatic;
import ikuyo.utils.MsgDiffer;
import ikuyo.utils.StarUtils;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class AreaBehavior implements Behavior<CommonContext> {
    @Override
    public void update(CommonContext context) {
        var updated = context.updated();
        if (updated.init()) {
            var states = context.areaStates;
            IntStream.range(0, states.size()).parallel().forEach(i -> {
                var state = states.get(i);
                var blocks = StarUtils.getBlocksAt(i);
                writeCache(context, state, blocks);
            });
        }
        var loadList = new ConcurrentSkipListSet<Integer>();
        var enableList = new ConcurrentSkipListSet<Integer>();
        updated.users().stream().parallel().forEach(id -> {
            var info = context.getInfo(id);
            if (info == null || !info.online) return;
            StarUtils.areasAround(info.x, info.y, MsgDiffer.cacheRange / Drawable.scaling).forEach(area -> {
                var state = context.areaStates.get(area);
                if (!state.loaded) {
                    state.loaded = true;
                    loadList.add(area);
                }
                if (AreaState.workSet) enableList.add(area);
            });
        });
        loadList.forEach(area -> {
            var blocks = StarUtils.getBlocksAt(area);
            updated.blocks().addAll(blocks);
        });
        if (AreaState.workSet) {
            context.engine().bullets.values().stream().parallel().forEach(bullet -> {
                var pos = bullet.getBody().getWorldCenter();
                StarUtils.areasAround(pos.x, pos.y, StarUtils.areaSize * 2 + 1).forEach(area -> {
                    enableList.add(area);
                });
            });
            var delta = new AtomicInteger(0);
            enableList.stream().parallel().forEach(area -> {
                if (context.enabledAreas.contains(area)) return;
                delta.incrementAndGet();
                StarUtils.getBlocksAt(area).forEach(id -> {
                    if (context.star().starInfo().blocks[id].isSurface)
                        context.engine().surfaceBlocks.get(id).setEnabled(true);
                });
                var state = context.areaStates.get(area);
                state.enabled = true;
            });
            context.enabledAreas.stream().parallel().forEach(area -> {
                if (enableList.contains(area)) return;
                delta.incrementAndGet();
                StarUtils.getBlocksAt(area).forEach(id -> {
                    if (context.star().starInfo().blocks[id].isSurface)
                        context.engine().surfaceBlocks.get(id).setEnabled(false);
                });
                var state = context.areaStates.get(area);
                state.enabled = false;
            });
            context.areaDelta = delta.getAcquire();
            context.enabledAreas = enableList;
        }
        if (context.writeBackLock.get()) return;
        updated.areas.stream().parallel().forEach(area -> {
            writeCache(context, context.areaStates.get(area), StarUtils.getBlocksAt(area));
        });
        updated.areas = new HashSet<>();
    }

    private static void writeCache(CommonContext context, AreaState state, List<Integer> blocks) {
        var mapper = DataStatic.mapper;
        blocks.forEach(i -> {
            try {
                state.cached.put(i, mapper.writeValueAsString(context.star().starInfo().blocks[i]));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        });
    }
}
