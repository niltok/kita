package ikuyo.server.behaviors;

import ikuyo.api.behaviors.Behavior;
import ikuyo.api.datatypes.Block;
import ikuyo.api.datatypes.Damage;
import ikuyo.api.datatypes.StarInfo;
import ikuyo.api.datatypes.UserInfo;
import ikuyo.server.api.*;
import ikuyo.utils.MsgDiffer;
import ikuyo.utils.Position;
import ikuyo.utils.StarUtils;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.AABB;
import org.dyn4j.geometry.Ray;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.result.DetectResult;

import java.util.Iterator;

public class BulletBehavior implements Behavior<CommonContext> {
    @Override
    public void update(CommonContext context) {
        context.engine().bullets.forEach((id, bullet) -> {
            if (bullet != null) {
                bullet.update(context);
                if (!bullet.ifHasEntity) {
                    // TODO: 2023/6/24 damage
                    Laser laser = (Laser) bullet;
                    var direction = laser.end.copy().subtract(laser.start);
                    laser.end = laser.start.copy().add(direction.copy().multiply(MsgDiffer.cacheRange));
                    var rayCast = context.engine().rayCast(new Ray(laser.start, direction),
                            Math.hypot(laser.end.x - laser.start.x, laser.end.y - laser.start.y),
                            null);

                    if (rayCast.isPresent()) {
                        laser.end = rayCast.get().copy().getRaycast().getPoint();
                        if (rayCast.get().getBody().getFixture(0).getFilter().equals(PhysicsEngine.USER)) {
//                            int userId = (int) rayCast.get().getBody().getUserData();
                            inflictDamage(rayCast.get().getBody(), laser.getDamage(), context);
                        }
                    }
                }
                else {
                    Iterator<DetectResult<KitasBody, BodyFixture>> iterator =
                            context.engine().broadPhaseDetect(bullet.body, null);
                    if (iterator.hasNext() && context.engine().ManifoldDetect(bullet.body, iterator)) {
                        bulletHandler(bullet, context);
                        context.engine().removeBullet(id);
                    }
                }
            }
        });
    }

    private void bulletHandler(Bullet bullet, CommonContext context) {
        userHandler(bullet.body.getWorldCenter(), bullet.damage, context);
        if (bullet.damage.ifBreakBlock) {
            blockHandler(bullet, context);
        }
    }

    public void userHandler(Vector2 position, Damage damage, CommonContext context) {
        Iterator<DetectResult<KitasBody, BodyFixture>> userIterator =
                context.engine().broadPhaseDetect(new AABB(position, damage.range),
                        filter -> filter.equals(PhysicsEngine.USER));
        while (userIterator.hasNext())
            inflictDamage(userIterator.next().getBody(), damage, context);
    }

    private static void inflictDamage(KitasBody body, Damage damage, CommonContext context) {
        //            todo: check damage position
        UserInfo userInfo = context.star().starInfo().starUsers.get((int) body.getUserData());
        double shieldDamage = damage.shieldOnlyDamage,
                hpDamage = damage.hpOnlyDamage,
                sanDamage = damage.sanDamage;

        if (userInfo.spaceship.shield - shieldDamage >= damage.normalDamage)
            shieldDamage += damage.normalDamage;
        else {
            double remain = Math.max(userInfo.spaceship.shield - shieldDamage, 0);
            shieldDamage += remain;
            hpDamage += damage.normalDamage - remain;
        }

        var userData = context.star().starInfo().starUsers.get((int) body.getUserData());
        userData.san = Math.max(userData.san - sanDamage, 0);
        userInfo.spaceship.inflict(shieldDamage, hpDamage);

        if (userInfo.san == 0) userInfo.controlType = "destroyed";
        if (userInfo.spaceship.hp == 0) {
            // TODO: 2023/6/26 死了啦！都是你害的！
            userInfo.controlType = "destroyed";
        }
    }

    private void blockHandler(Bullet bullet, CommonContext context) {
        var starInfo = context.star().starInfo();
        var position = bullet.body.getWorldCenter();
        var damage = bullet.damage;
        int userId = (int) bullet.body.getUserData();
        var userInfo = context.getInfo(userId);
        int[] blocklist = StarUtils.nTierAround(Position.from(position), damage.range, false)
                .stream().mapToInt(Integer::valueOf).toArray();
        for (var b : blocklist) {
            if (starInfo.blocks[b].isDestructible) {
                if (starInfo.blocks[b].isSurface) {
                    context.engine().removeBody(context.engine().surfaceBlocks.get(b));
                    context.engine().surfaceBlocks.remove(b);
                }

                userInfo.spaceship.cargoHold.put(starInfo.blocks[b].drop);

                starInfo.blocks[b] = new Block.Normal();
                context.updated().blocks().add(b);
                context.updated().areas.add(StarUtils.getAreaOf(StarUtils.realIndexOf(b)));
            }
        }

        for (var i : StarUtils.surfaceBlocks(
                StarUtils.realIndexOf(position.x, position.y),
                (int) ((damage.range + StarInfo.edgeLength) * Math.sqrt(3) / 2 / StarInfo.tierDistance) - 1,
                (int) ((damage.range + StarInfo.edgeLength) / StarInfo.tierDistance) + 2,
                starInfo)) {

            if (!starInfo.blocks[i].isSurface) {
                starInfo.blocks[i].isSurface = true;
                starInfo.blocks[i].isCollisible = true;
                context.engine().addBlock(i);
                context.updated().blocks().add(i);
            }
            context.updated().areas.add(StarUtils.getAreaOf(StarUtils.realIndexOf(i)));
        }
    }
}
