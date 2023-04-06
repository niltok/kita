package ikuyo.server.renderers;

import ikuyo.api.datatypes.Drawable;
import ikuyo.api.datatypes.StarInfo;
import ikuyo.api.entities.Star;
import ikuyo.server.api.CommonContext;
import ikuyo.utils.MsgDiffer;
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

        context.updated().users().forEach(id -> {
            var info = context.getInfo(id);
            if (info == null || !info.online) return;
            StarUtils.areasAround(info.x, info.y, MsgDiffer.cacheRange / Drawable.scaling).forEach(area -> {
                var state = context.areaStates.get(area);
                if (state.loaded) return;
                state.loaded = true;
                update.addAll(StarUtils.getBlocksAt(area));
            });
//            var area = StarUtils.getAreaOf(StarUtils.realIndexOf(info.x, info.y));
//            var state = context.areaStates().get(area);
//            if (state.loaded) return;
//            state.loaded = true;
//            update.addAll(StarUtils.getBlocksAt(area));
        });

        update.forEach(id -> renderBlock(drawables, star, id));

//        showAreas(context, drawables, 0 , 36, "rainbow");
//
//        if (context.updated().init) {
//            Vector2 start = StarUtils.areaToBlock(-2.6576347892342223, -2.7457436564763738, 15);
//            Vector2 end = StarUtils.areaToBlock(-1.266666666666666, -1.3086606101631508, 15);
//            var line = new Drawable.Line().setWidth(2).setColor(0xff0000)
//                    .setStartPoint(start.x * Drawable.scaling, start.y * Drawable.scaling)
//                    .setEndPoint(end.x * Drawable.scaling, end.y * Drawable.scaling);
//            line.zIndex = 5;
//            drawables.put("debug.line-#light", line);
//
//            for (int i = 0; i <= 36; i++)
//                drawAreaOutline(drawables, i);
//        }
    }

    private static void drawAreaOutline(Map<String, Drawable> drawables, int area) {
        Vector2 center = StarUtils.vPositionOf(area);
        Drawable.Line line;
        Vector2[] vertices = new Vector2[6];
        for (int i = 0; i < 6; i++) {
            Vector2 vertex = new Vector2(StarInfo.edgeLength, 0)
                    .rotate(Math.PI / 3 * i + Math.PI / 6)
                    .add(center);
            vertices[i] = StarUtils.areaToBlock(vertex.x, vertex.y, 15);
        }
        for (int i = 0; i < 6; i++) {
            line = new Drawable.Line().setWidth(5).setColor(0xff0000)
                    .setStartPoint(vertices[i].x * Drawable.scaling, vertices[i].y * Drawable.scaling)
                    .setEndPoint(vertices[(i + 1) % 6].x * Drawable.scaling, vertices[(i + 1) % 6].y * Drawable.scaling);
            line.zIndex = 4;
            drawables.put("debug.line-#area%d-#%d".formatted(area, i), line);
        }
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
