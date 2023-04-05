package ikuyo.server.renderers;

import ikuyo.api.datatypes.Drawable;
import ikuyo.api.entities.Star;
import ikuyo.server.api.CommonContext;
import ikuyo.utils.Position;
import ikuyo.utils.StarUtils;
import io.vertx.core.buffer.Buffer;

import java.util.HashSet;
import java.util.Map;

public class BlockRenderer implements DrawablesRenderer {
    public static boolean surfaceOnly = false;
    @Override
    public void renderDrawables(CommonContext context, Map<String, Drawable> drawables) {
        var star = context.star();
        var update = new HashSet<>(context.updated().blocks());

//        context.updated().users().forEach(id -> {
//            var info = context.getInfo(id);
//            if (info == null || !info.online) return;
//            StarUtils.areasAround(info.x, info.y, MsgDiffer.cacheRange / Drawable.scaling).forEach(area -> {
//                var state = context.areaStates().get(area);
//                if (state.loaded) return;
//                state.loaded = true;
//                update.addAll(StarUtils.getBlocksAt(area));
//            });
////            var area = StarUtils.getAreaOf(StarUtils.realIndexOf(info.x, info.y));
////            var state = context.areaStates().get(area);
////            if (state.loaded) return;
////            state.loaded = true;
////            update.addAll(StarUtils.getBlocksAt(area));
//        });
//
//        update.forEach(id -> renderBlock(drawables, star, id));

        showAreas(context, drawables, 7 , 7);
        markDown(drawables, 10664, "40-4");
    }

    private static void renderBlock(Map<String, Drawable> drawables, Star star, int i) {
        var block = star.starInfo().blocks[i];
        if (block.isVisible && (!surfaceOnly || block.isSurface)) {
            var d = new Drawable.Sprite();
            Position pos = StarUtils.positionOf(StarUtils.realIndexOf(i));
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

    private static void markDown(Map<String, Drawable> drawables, int i, String asset) {
        var d = new Drawable.Sprite();
        Position pos = StarUtils.positionOf(StarUtils.realIndexOf(i));
        d.x = pos.x * Drawable.scaling;
        d.y = pos.y * Drawable.scaling;
        d.bundle = "blocks";
        d.asset = asset;
        drawables.put(Buffer.buffer()
                .appendString("block#")
                .appendString(String.valueOf(i))
                .appendString(".image").toString(), d);
    }

    private static void showAreas(CommonContext context, Map<String, Drawable> drawables, int start, int end) {
        for (int area = start; area <= end; area++) {
            var state = context.areaStates().get(area);
            if (!state.loaded) {
                state.loaded = true;
                for (var id : StarUtils.getBlocksAt(area)) {
                    if (StarUtils.getAreaOf(StarUtils.realIndexOf(id)) != area)
                        markDown(drawables, id, "40-4");
                    else markDown(drawables, id, "0-0");
                }
            }
        }
    }
}
