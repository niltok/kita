package ikuyo.server.behaviors;

import ikuyo.api.Block;
import ikuyo.api.Position;
import ikuyo.api.StarInfo;
import ikuyo.api.behaviors.Behavior;
import ikuyo.server.api.Bullet;
import ikuyo.server.api.CommonContext;
import ikuyo.server.api.KitasBody;
import ikuyo.server.api.PhysicsEngine;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.AABB;
import org.dyn4j.geometry.Vector2;
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
                    bulletHandler(bullet, context);
                    context.engine().removeBullet(id);
                }
            }
        });
    }

    private void bulletHandler(Bullet bullet, CommonContext context) {
        switch (bullet.type) {
            case "defaultWeapon", default -> {
                userHandler(bullet.body.getWorldCenter(), bullet.range, bullet.damage, context);
                blockHandler(bullet.body.getWorldCenter(), bullet.range, context);
            }
            case "r400" -> {
                userHandler(bullet.body.getWorldCenter(), bullet.range, bullet.damage, context);
            }
        }
    }

    private void userHandler(Vector2 position, double range, double damage, CommonContext context) {
        Iterator<DetectResult<KitasBody, BodyFixture>> userIterator =
                context.engine().broadPhaseDetect(new AABB(position, range),
                        filter -> filter.equals(PhysicsEngine.USER));
        while (userIterator.hasNext()) {
            var body = userIterator.next().getBody();
            var userPos = body.getWorldCenter();
//            todo: damage position
            if (userPos.distance(position) <= range) {
                StarInfo.StarUserInfo userInfo = context.star().starInfo().starUsers.get((int)body.getUserData());
                if (userInfo.spaceship.shield >= damage)
                    userInfo.spaceship.shield -= damage;
                else if (userInfo.spaceship.shield > 0) {
                    userInfo.spaceship.hp -= damage - userInfo.spaceship.shield;
                    userInfo.spaceship.shield = 0;
                } else
                    userInfo.spaceship.hp -= damage;

                if (userInfo.spaceship.hp <= 0) {
                    userInfo.spaceship.hp = 0;
                    userInfo.controlType = "destroyed";
                }
            }
        }
    }

    private void blockHandler(Vector2 position, double range, CommonContext context) {
        var starInfo = context.star().starInfo();

        int[] blocklist = StarInfo.nTierAround(new Position(position.x, position.y), range)
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
                StarInfo.realIndexOf(position.x, position.y),
                (int) ((range + StarInfo.edgeLength) * Math.sqrt(3) / 2 / StarInfo.tierDistance) - 1,
                (int) ((range + StarInfo.edgeLength) / StarInfo.tierDistance) + 2,
                starInfo)) {

            starInfo.blocks[i].isSurface = true;
            starInfo.blocks[i].isCollisible = true;
            context.engine().addBlock(i);
            context.updated().blocks().add(i);
        }
    }
}
