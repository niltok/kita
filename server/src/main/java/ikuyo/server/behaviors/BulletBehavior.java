package ikuyo.server.behaviors;

import ikuyo.api.Block;
import ikuyo.api.Position;
import ikuyo.api.StarInfo;
import ikuyo.api.behaviors.Behavior;
import ikuyo.server.api.CommonContext;
import ikuyo.server.api.KitasBody;
import ikuyo.server.api.PhysicsEngine;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.AABB;
import org.dyn4j.world.result.DetectResult;

import java.util.Iterator;

public class BulletBehavior implements Behavior<CommonContext> {
    @Override
    public void update(CommonContext context) {

        context.engine().bullets.forEach((id, bullet) -> {
            if (bullet != null) {
                Iterator<DetectResult<KitasBody, BodyFixture>> iterator =
                        context.engine().broadPhaseDetect(bullet.body, null);
                if (iterator.hasNext() && context.engine().ManifoldDetect(bullet.body, iterator)) {
                    var starInfo = context.star().starInfo();
                    var pos = bullet.body.getWorldCenter();
                    double range = bullet.range;

                    Iterator<DetectResult<KitasBody, BodyFixture>> userIterator =
                            context.engine().broadPhaseDetect(new AABB(pos, range),
                                    filter -> filter.equals(PhysicsEngine.USER));
                    while (userIterator.hasNext()) {
                        var body = userIterator.next().getBody();
                        var userPos = body.getWorldCenter();
                        if (userPos.distance(pos) <= range) {
                            StarInfo.StarUserInfo userInfo = context.star().starInfo().starUsers.get((int)body.getUserData());
                            if (userInfo.Shield >= bullet.damage)
                                userInfo.Shield -= bullet.damage;
                            else if (userInfo.Shield > 0) {
                                userInfo.HP -= bullet.damage - userInfo.Shield;
                                userInfo.Shield = 0;
                            } else
                                userInfo.HP -= bullet.damage;

                            if (userInfo.HP <= 0) {
                                userInfo.HP = 0;
                                userInfo.controlType = "destroyed";
                            }

//                            System.out.printf("[HP]: %f\n", context.star().starInfo().starUsers.get((int)body.getUserData()).HP);
                        }
                    }


                    int[] blocklist = StarInfo.nTierAround(new Position(pos.x, pos.y), range)
                            .stream().mapToInt(Integer::valueOf).toArray();
                    for (var b : blocklist) {
                        if (starInfo.blocks[b].isDestructible) {
                            if (starInfo.blocks[b].isSurface) {
                                context.engine().removeBody(context.engine().surfaceBlocks.get(b));
                                context.engine().surfaceBlocks.remove(b);
                            }
                            starInfo.blocks[b] = new Block.Normal();
                            context.updated().blocks().add(b);
                        }
                    }

                    for (var i : StarInfo.surfaceBlocks(
                            StarInfo.realIndexOf(pos.x, pos.y),
                            (int) ((range + StarInfo.edgeLength) * Math.sqrt(3) / 2 / StarInfo.tierDistance) - 1,
                            (int) ((range + StarInfo.edgeLength) / StarInfo.tierDistance) + 2,
                            starInfo)) {

                        starInfo.blocks[i].isSurface = true;
                        starInfo.blocks[i].isCollisible = true;
                        context.engine().addBlock(i);
                        context.updated().blocks().add(i);
                    }

                    context.engine().removeBullet(id);
                }
            }
        });
    }
}
