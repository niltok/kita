package ikuyo.server.behaviors;

import ikuyo.api.behaviors.Behavior;
import ikuyo.api.cargo.CargoStatic;
import ikuyo.api.datatypes.Damage;
import ikuyo.api.datatypes.UserInput;
import ikuyo.api.equipments.Weapon;
import ikuyo.api.equipments.WeaponItem;
import ikuyo.server.api.Bullet;
import ikuyo.server.api.CommonContext;
import ikuyo.server.api.KitasBody;
import ikuyo.server.api.PhysicsEngine;
import ikuyo.utils.Position;
import org.dyn4j.geometry.Ray;
import org.dyn4j.geometry.Vector2;

import java.util.Objects;

public class UserAttackBehavior implements Behavior<CommonContext> {

    @Override
    public void update(CommonContext context) {
        context.updated().users().forEach((id) -> {
            var state = context.getState(id);
            var userInfo = context.getInfo(id);
            if (state == null || userInfo == null || !userInfo.online) return;
            var input = state.input;

            if (input.shot > 0 && userInfo.spaceship != null && userInfo.spaceship.getCurrentWeapon() != null
                    && userInfo.spaceship.tryFire())
                shot(id, input, userInfo.spaceship.getCurrentWeapon(), context);
        });
    }

    private void shot(Integer id, UserInput input, Weapon weapon, CommonContext context) {
        BulletInfo info = new BulletInfo();
        info.type = weapon.type;
        info.userId = id;
        Bullet bullet;

        if (weapon.type.equals(CargoStatic.chargeRifle.type())) bullet = new Bullet.Laser();
        else bullet = new Bullet();

        Position point = input.pointAt;
        Vector2 userPos = new Vector2(context.star().starInfo().starUsers.get(id).x,
                context.star().starInfo().starUsers.get(id).y);
        double radius = context.engine().users.get(id).getBody().getRotationDiscRadius();
        Vector2 direction = new Vector2(point.x - userPos.x, point.y - userPos.y).getNormalized();

        info.set(
                weapon.getInfo().collisionRange,
                direction.copy().multiply(radius + weapon.getInfo().collisionRange).add(userPos),
                direction.copy().multiply(weapon.getInfo().velocity),
                weapon.getDamage()
        );

        if (weapon.type.equals(CargoStatic.r400.type()))
            info.gravityScale = 0.01;

        if (weapon.type.equals(CargoStatic.chargeRifle.type()))
            info.userBody = context.engine().users.get(id).getBody();

//        v.add(context.engine().users.get(id).getValue().getLinearVelocity());
        info.bulletCheck(userPos, direction, radius, context);
        bullet.set(info, context);
        context.engine().addBullet(bullet);
    }

    public static class BulletInfo {
        public String type;
        public int userId;
        public double r;
        public Vector2 pos;
        public Vector2 velocity;
        public Damage damage;
        public double gravityScale = 1.0;
        public KitasBody userBody;
        public long liveTime;

        public void set(double bulletR, Vector2 bulletPos, Vector2 bulletVelocity, Damage damage) {
            this.r = bulletR;
            this.pos = bulletPos;
            this.velocity = bulletVelocity;
            this.damage = damage;
            this.liveTime = Objects.requireNonNull(WeaponItem.get(this.type)).ammoType.liveTime;
        }

        private void bulletCheck(Vector2 userPos, Vector2 direction, double radius, CommonContext context) {
            var rayCast = context.engine().rayCast(new Ray(userPos, direction),
                            radius + this.r * 2, filter -> filter.equals(PhysicsEngine.BLOCK));
            if (rayCast.isPresent()) {
                this.pos = rayCast.get().copy().getRaycast().getPoint()
                        .subtract(direction.copy().multiply(this.r));
                this.velocity = new Vector2();
            }
        }
    }
}
