package ikuyo.server.renderers;

import ikuyo.api.datatypes.Drawable;
import ikuyo.api.entities.Star;
import ikuyo.server.api.CommonContext;
import ikuyo.utils.Position;
import ikuyo.utils.StarUtils;
import io.vertx.core.buffer.Buffer;
import org.dyn4j.geometry.Vector2;

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
//                var state = context.areaStates.get(area);
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

        showAreas(context, drawables, 0 , 36, "rainbow");

        var drawable = new Drawable.Line();
        drawable.width = 2;
        drawable.color = 0xff0000;

        Vector2 trans = StarUtils.areaToBlock(-2.6576347892342223, -2.7457436564763738, 15);
        markDown(drawables, StarUtils.indexOf(
                StarUtils.realIndexOf(trans.x, trans.y)), "40-4");

        drawable.x = trans.x * Drawable.scaling;
        drawable.y = trans.y * Drawable.scaling;

        trans = StarUtils.areaToBlock(-1.266666666666666, -1.3086606101631508, 15);
        markDown(drawables, StarUtils.indexOf(
                StarUtils.realIndexOf(trans.x, trans.y)), "40-4");

        drawable.lineTo(trans.x * Drawable.scaling, trans.y * Drawable.scaling);

        drawables.put("debug.line", drawable);

//        markDown(drawables, 10664, "40-4");
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

    private static void showAreas(CommonContext context, Map<String, Drawable> drawables, int start, int end, String asset) {
        for (int area = start; area <= end; area++) {
            var state = context.areaStates.get(area);
            if (!state.loaded) {
                state.loaded = true;
                for (var id : StarUtils.getBlocksAt(area))
                    markDown(drawables, id, asset.equals("rainbow") ? "%d-0".formatted(area % 6) : asset);
            }
        }
    }
}
