package ikuyo.server.renderers;

import ikuyo.api.Drawable;
import ikuyo.api.Position;
import ikuyo.api.Star;
import ikuyo.api.StarInfo;
import ikuyo.server.api.CommonContext;
import ikuyo.utils.MsgDiffer;
import io.vertx.core.buffer.Buffer;

import java.util.HashSet;
import java.util.Map;

public class BlockRenderer implements DrawablesRenderer {
    public static boolean surfaceOnly = false;
    @Override
    public void renderDrawables(CommonContext context, Map<String, Drawable> drawables) {
        var star = context.star();
        var update = new HashSet<>(context.updated().blocks());

        context.updated().users().forEach(id -> {
            var info = context.getInfo(id);
            if (info == null || !info.online) return;
            StarInfo.areasAround(info.x, info.y, MsgDiffer.cacheRange / Drawable.scaling).forEach(area -> {
                var state = context.areaStates().get(area);
                if (state.loaded) return;
                state.loaded = true;
                update.addAll(StarInfo.getBlocksAt(area));
            });
//            var area = StarInfo.getAreaOf(StarInfo.realIndexOf(info.x, info.y), StarInfo.areaSize);
//            var state = context.areaStates().get(area);
//            if (state.loaded) return;
//            state.loaded = true;
//            update.addAll(StarInfo.getBlocksAt(area));
        });

        update.forEach(id -> renderBlock(drawables, star, id));
    }

    private static void renderBlock(Map<String, Drawable> drawables, Star star, int i) {
        var block = star.starInfo().blocks[i];
        if (block.isVisible && (!surfaceOnly || block.isSurface)) {
            var d = new Drawable.Sprite();
            Position pos = StarInfo.posOf(StarInfo.realIndexOf(i, star.starInfo().minTier));
//                System.out.println("[x]: %f, [y]: %f".formatted(pos.x, pos.y));
            d.x = pos.x * Drawable.scaling;
            d.y = pos.y * Drawable.scaling;
//            var suf = context.engine().surfaceBlocks.get(i);
//            if (suf != null) d.rotation = suf.getTransform().getRotationAngle();
            d.bundle = "blocks";
            d.asset = block.type + "-" + block.variant;
            drawables.put(Buffer.buffer()
                    .appendString("block#")
                    .appendString(String.valueOf(i))
                    .appendString(".image").toString(), d);
        } else {
            drawables.put(Buffer.buffer()
                    .appendString("block#")
                    .appendString(String.valueOf(i))
                    .appendString(".image").toString(), null);

        }
    }
}
