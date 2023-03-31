package ikuyo.server.behaviors;

import ikuyo.api.Damage;
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

            if (input.shot > 0 && userInfo.spaceship.getCurrentWeapon() != null
                    && userInfo.spaceship.tryFire())
                shot(id, input, userInfo.spaceship.getCurrentWeapon(), context);
        });
    }

    private void shot(Integer id, UserInput input, AbstractWeapon weapon, CommonContext context) {
        BulletInfo info = new BulletInfo();
        info.type = weapon.type;
        info.userId = id;
        Bullet bullet = new Bullet();

        Position point = input.pointAt;
        Vector2 userPos = new Vector2(context.star().starInfo().starUsers.get(id).x,
                context.star().starInfo().starUsers.get(id).y);
        double radius = context.engine().users.get(id).getValue().getRotationDiscRadius();
        Vector2 direction = new Vector2(point.x - userPos.x, point.y - userPos.y).getNormalized();


        info.set(
                weapon.getInfo().collisionRange,
                direction.copy().multiply(radius + weapon.getInfo().collisionRange).add(userPos),
                direction.copy().multiply(weapon.getInfo().velocity),
                weapon.getDamage()
        );

//        v.add(context.engine().users.get(id).getValue().getLinearVelocity());
        info.bulletCheck(userPos, direction, radius, context);
        bullet.set(info);
        context.engine().addBullet(bullet);
    }

    public static class BulletInfo {
        public String type;
        public int userId;
        public double r;
        public Vector2 pos;
        public Vector2 velocity;
        public Damage damage;

        public void set(double bulletR, Vector2 bulletPos, Vector2 bulletVelocity, Damage damage) {
            this.r = bulletR;
            this.pos = bulletPos;
            this.velocity = bulletVelocity;
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
