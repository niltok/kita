package ikuyo.server.behaviors;

import ch.ethz.globis.phtree.PhTreeF;
import com.fasterxml.jackson.core.JsonProcessingException;
import ikuyo.api.behaviors.Behavior;
import ikuyo.api.datatypes.Drawable;
import ikuyo.server.api.AreaState;
import ikuyo.server.api.CommonContext;
import ikuyo.server.api.UpdatedContext;
import ikuyo.utils.DataStatic;
import ikuyo.utils.MsgDiffer;
import ikuyo.utils.StarUtils;

import java.util.Comparator;
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
            IntStream.range(0, states.size()).forEach(i -> {
                var apos = StarUtils.positionOf(i);
                var bpos = StarUtils.areaToBlock(apos.x, apos.y);
                context.loadingArea.put(new double[] {bpos.x, bpos.y}, i);
            });
        }
        lazyLoad(context, updated);
        if (AreaState.workSet) {
            updateWorkSet(context, updated);
        }
        if (context.writeBackLock.compareAndExchange(false, true)) return;
        updated.areas.stream().parallel().forEach(area -> {
            writeCache(context, context.areaStates.get(area), StarUtils.getBlocksAt(area));
        });
        updated.areas = new HashSet<>();
        context.writeBackLock.set(false);
    }

    private static void updateWorkSet(CommonContext context, UpdatedContext updated) {
        var enableList = new ConcurrentSkipListSet<Integer>();
        updated.users().stream().parallel().forEach(id -> {
            var info = context.getInfo(id);
            if (info == null || !info.online) return;
            StarUtils.areasAround(info.x, info.y, StarUtils.areaSize * 2 + 1).forEach(area -> {
                enableList.add(area);
            });
        });
        var engine = context.engine();
        engine.bullets.values().stream().parallel().forEach(bullet -> {
            if (bullet == null) return;
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
                var block = context.star().starInfo().blocks[id];
                if (block.isSurface && block.isCollisible)
                    engine.enableBody(engine.surfaceBlocks.get(id));
            });
            var state = context.areaStates.get(area);
            state.enabled = true;
        });
        context.enabledAreas.stream().parallel().forEach(area -> {
            if (enableList.contains(area)) return;
            delta.incrementAndGet();
            StarUtils.getBlocksAt(area).forEach(id -> {
                var block = context.star().starInfo().blocks[id];
                if (block.isSurface && block.isCollisible)
                    engine.disableBody(engine.surfaceBlocks.get(id));
            });
            var state = context.areaStates.get(area);
            state.enabled = false;
        });
        context.areaDelta = delta.getAcquire();
        context.enabledAreas = enableList;
    }

    private static void lazyLoad(CommonContext context, UpdatedContext updated) {
        var loadList = new ConcurrentSkipListSet<PhTreeF.PhEntryF<Integer>>(
                Comparator.comparingInt(PhTreeF.PhEntryF::getValue));
        updated.users().stream().parallel().forEach(id -> {
            var info = context.getInfo(id);
            if (info == null || !info.online) return;
            var r = MsgDiffer.cacheRange / Drawable.scaling + StarUtils.areaSize * 2;
            context.loadingArea.queryAll(new double[]{info.x - r, info.y - r}, new double[]{info.x + r, info.y + r})
                    .forEach(entry -> {
                var area = entry.getValue();
                var state = context.areaStates.get(area);
                if (!state.loaded) {
                    state.loaded = true;
                    loadList.add(entry);
                }
            });
        });
        loadList.forEach(entry -> {
            context.loadingArea.remove(entry.getKey());
            var blocks = StarUtils.getBlocksAt(entry.getValue());
            updated.blocks().addAll(blocks);
        });
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
