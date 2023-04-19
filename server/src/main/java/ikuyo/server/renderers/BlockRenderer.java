package ikuyo.server.renderers;

import ikuyo.api.datatypes.Drawable;
import ikuyo.api.datatypes.StarInfo;
import ikuyo.api.entities.Star;
import ikuyo.server.api.CommonContext;
import ikuyo.utils.Position;
import ikuyo.utils.StarUtils;
import io.vertx.core.buffer.Buffer;
import org.dyn4j.geometry.Vector2;

import java.util.Map;

public class BlockRenderer implements DrawablesRenderer {
    public static boolean surfaceOnly = false;
    @Override
    public void renderDrawables(CommonContext context, Map<String, Drawable> drawables) {
        var star = context.star();
        context.updated().blocks().forEach(id -> renderBlock(drawables, star, id));
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
