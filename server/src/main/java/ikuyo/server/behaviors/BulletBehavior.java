package ikuyo.server.behaviors;

import ikuyo.api.Block;
import ikuyo.api.Position;
import ikuyo.api.StarInfo;
import ikuyo.api.behaviors.Behavior;
import ikuyo.server.api.CommonContext;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.world.result.DetectResult;

import java.util.ArrayList;
import java.util.Iterator;

public class BulletBehavior implements Behavior<CommonContext> {
    @Override
    public void update(CommonContext context) {
        var remove = new ArrayList<String>();

        context.engine().bullets.forEach((id, bullet) -> {
            if (bullet == null) remove.add(id);
            else {
                Iterator<DetectResult<Body, BodyFixture>> iterator =
                        context.engine().broadPhaseDetect(bullet);
                if ( iterator.hasNext() && context.engine().ManifoldDetect(bullet, iterator) ) {
                    var starInfo = context.star().starInfo();
                    var pos = bullet.body.getWorldCenter();
                    double r = 10;

//                    todo: damage

                    int[] blocklist = StarInfo.nTierAround(new Position(pos.x, pos.y), r)
                            .stream().mapToInt(Integer::valueOf).toArray();
                    for (var b: blocklist) {
                        if (starInfo.blocks[b].isDestructible) {
                            if (starInfo.blocks[b].isSurface) {
                                context.engine().removeBody(context.engine().surfaceBlocks.get(b));
                                context.engine().surfaceBlocks.remove(b);
                            }
                            starInfo.blocks[b] = new Block.Normal();
                            context.updated().blocks().add(b);
                        }
                    }

                    for (var i: StarInfo.surfaceBlocks(
                            StarInfo.realIndexOf(pos.x, pos.y),
                            (int)((r + StarInfo.edgeLength) * Math.sqrt(3) / 2 / StarInfo.tierDistance) - 1,
                            (int)((r + StarInfo.edgeLength) / StarInfo.tierDistance) + 2,
                            starInfo)) {

                        starInfo.blocks[i].isSurface = true;
                        starInfo.blocks[i].isCollisible = true;
                        context.engine().addBlock(i);
                        context.updated().blocks().add(i);
                    }

                    context.engine().removeBody(bullet.body);
                    context.engine().bullets.put(id, null);
                }
            }
        });

        for (var id : remove)
            context.engine().bullets.remove(id);
    }
}
