package ikuyo.server.behaviors;

import ikuyo.api.Position;
import ikuyo.api.UserInput;
import ikuyo.api.behaviors.Behavior;
import ikuyo.api.equipments.AbstractWeapon;
import ikuyo.server.api.Bullet;
import ikuyo.server.api.CommonContext;
import ikuyo.server.api.PhysicsEngine;
import org.dyn4j.geometry.Ray;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.result.RaycastResult;

public class UserAttackBehavior implements Behavior<CommonContext> {

    @Override
    public void update(CommonContext context) {
        context.userInputs().forEach((id, input) -> {
            var userInfo = context.star().starInfo().starUsers.get(id);
            if (!userInfo.online) return;

            if (input.shot == 1 && userInfo.weapon != null)
                shot(id, input, userInfo.weapon, context);
        });
    }

    private void shot(Integer id, UserInput input, AbstractWeapon weapon, CommonContext context) {
       BulletInfo info = new BulletInfo();

        Position point = input.pointAt;
        Vector2 userPos = new Vector2(context.star().starInfo().starUsers.get(id).x,
                context.star().starInfo().starUsers.get(id).y);
        double radius = context.engine().users.get(id).getValue().getRotationDiscRadius();
        Vector2 direction = new Vector2(point.x - userPos.x, point.y - userPos.y).getNormalized();

        switch (weapon.type) {
            case "defaultWeapon" -> {
                info.set(
                        0.3,
                        direction.copy().multiply(radius + 0.3).add(userPos),
                        direction.copy().multiply(150),
                        5, weapon.getDamage()
                );
            }
            case "" -> {
                info.set(
                        0.4,
                        direction.copy().multiply(radius + 0.4).add(userPos),
                        new Vector2(),
                        5, 100
                );
            }
            case default, null -> {
                return;
            }
        }

//        v.add(context.engine().users.get(id).getValue().getLinearVelocity());
        info.bulletCheck(userPos, direction, radius, context);

        Bullet bullet = context.engine().addBullet(info.pos, info.r);
        bullet.set(weapon.type, info.range, info.damage);
        bullet.body.setLinearVelocity(info.velocity);
        bullet.body.setUserData(id);
    }

    private static class BulletInfo {
        public double r;
        public Vector2 pos;
        public Vector2 velocity;
        public double range;
        public double damage;

        public void set(double bulletR, Vector2 bulletPos, Vector2 bulletVelocity, double range, double damage) {
            this.r = bulletR;
            this.pos = bulletPos;
            this.velocity = bulletVelocity;
            this.range = range;
            this.damage = damage;
        }

        private void bulletCheck(Vector2 userPos, Vector2 direction, double radius, CommonContext context) {
            var rayCast = context.engine().rayCast(new Ray(userPos, direction),
                            radius + this.r * 2, filter -> filter.equals(PhysicsEngine.BLOCK))
                    .stream().min(RaycastResult::compareTo);
            if (rayCast.isPresent()) {
                this.pos = rayCast.get().copy().getRaycast().getPoint()
                        .subtract(direction.copy().multiply(this.r));
                this.velocity = new Vector2();
            }
        }
    }
}
