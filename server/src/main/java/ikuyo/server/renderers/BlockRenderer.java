package ikuyo.server.renderers;

import ikuyo.api.Drawable;
import ikuyo.api.Position;
import ikuyo.api.Star;
import ikuyo.api.StarInfo;

import java.util.List;
import java.util.Map;

public class BlockRenderer implements DrawablesRenderer {
    @Override
    public void renderDrawables(Star star, Map<String, Drawable> drawables) {
        int rindex = StarInfo.realIndexOf(0, star.starInfo().mintier);
        for (var i = 0; i < star.starInfo().blocks.length; i++) {
            var block = star.starInfo().blocks[i];
            if (block.type == 1) {
                var d = new Drawable.Sprite();
                Position pos = StarInfo.posOf(rindex);
                d.x = pos.x * 20;
                d.y = pos.y * 20;
                d.bundle = "blocks";
                d.asset = String.valueOf(block.type);
                drawables.put("block#%d.image".formatted(i), d);
            }
            rindex++;
        }
    }
}
