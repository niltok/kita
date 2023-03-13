package ikuyo.server.renderers;

import ikuyo.api.Drawable;
import ikuyo.api.Position;
import ikuyo.api.Star;
import ikuyo.api.StarInfo;
import ikuyo.server.api.RendererContext;
import io.vertx.core.buffer.Buffer;

import java.util.Map;

public class BlockRenderer implements DrawablesRenderer {
    @Override
    public void renderDrawables(RendererContext context, Map<String, Drawable> drawables) {
        var star = context.common().star();
        if (!context.common().updated().init().get()) {
            context.common().updated().blocks().forEach(id -> {
                renderBlock(drawables, star, id);
            });
        }
        else for (var i = 0; i < star.starInfo().blocks.length; i++) {
//            if (context.common().engine().surfaceBlocks.containsKey(i))
            renderBlock(drawables, star, i);
        }
    }

    private static void renderBlock(Map<String, Drawable> drawables, Star star, int i) {
        var block = star.starInfo().blocks[i];
        if (block.isVisible) {
            var d = new Drawable.Sprite();
            Position pos = StarInfo.posOf(StarInfo.realIndexOf(i, star.starInfo().minTier));
//                System.out.println("[x]: %f, [y]: %f".formatted(pos.x, pos.y));
            d.x = pos.x * Drawable.scaling;
            d.y = pos.y * Drawable.scaling;
            d.bundle = "blocks";
            d.asset = block.type + "-" + block.variant;
            drawables.put(Buffer.buffer()
                    .appendString("block#")
                    .appendString(String.valueOf(i))
                    .appendString(".image").toString(), d);
        }
    }
}
