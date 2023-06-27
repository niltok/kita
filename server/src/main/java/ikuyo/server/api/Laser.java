package ikuyo.server.api;

import ikuyo.api.datatypes.Damage;
import ikuyo.api.datatypes.Drawable;
import ikuyo.server.behaviors.UserAttackBehavior;
import org.dyn4j.geometry.Ray;
import org.dyn4j.geometry.Vector2;

public class Laser extends Bullet {
    public Drawable.Line drawable;
    public Vector2 start;
    public Vector2 end;

    @Override
    public void set(UserAttackBehavior.BulletInfo info, CommonContext context) {
        this.ifHasEntity = false;

        body = info.userBody;
        this.type = info.type;
        this.damage = info.damage;
        this.drawable = new Drawable.Line();
        drawable.zIndex = 3;
        drawable.width = 3;
        drawable.color = 16764928;

        this.setFrame(80);
        this.update(context);
        this.updateDrawable();
    }

    @Override
    public void update(CommonContext context) {
        Vector2 userPos = this.body.getWorldCenter();
        this.end = context.getState((int) this.body.getUserData()).input.pointAt.toVector();
        Vector2 direction = new Vector2(end.x - userPos.x, end.y - userPos.y).getNormalized();
        this.start = direction.copy().multiply(this.body.getRotationDiscRadius()).add(userPos);

        if (end.copy().subtract(userPos).getMagnitude() < start.copy().subtract(userPos).getMagnitude())
            this.end = this.start.copy().add(direction);

        var rayCast = context.engine().rayCast(new Ray(userPos, direction),
                this.body.getRotationDiscRadius(), filter -> filter.equals(PhysicsEngine.BLOCK));
        rayCast.ifPresent(kitasBodyBodyFixtureRaycastResult ->
                this.start = kitasBodyBodyFixtureRaycastResult.copy().getRaycast().getPoint()
                        .copy().subtract(direction.multiply(0.00001)));
    }

    @Override
    public void updateDrawable() {
        if (frame == 0) {
            this.drawable = null;
            return;
        }
        drawable.x = start.x * Drawable.scaling;
        drawable.y = start.y * Drawable.scaling;

        drawable.lineTo(end.x * Drawable.scaling, end.y * Drawable.scaling);

        long count = 80 - frame;
        if (count <= 45) {
            drawable.width = (double) count / 45 + 3;
        } else if (count <= 53) {
            drawable.width = (double) (count - 45) / 8 * 6 + 4;
        } else {
            drawable.width = 10 - (double) (count - 53) / 27 * 10;
        }
    }

    @Override
    public Drawable getDrawable() {
        return this.drawable;
    }

    public Damage getDamage() {
        long count = 80 - frame;
        if (count < 45) {
            if (count % 3 == 0) {
                this.damage.normalDamage = 3;
                this.damage.sanDamage = 1;
            } else {
                this.damage.normalDamage = 0;
                this.damage.sanDamage = 0;
            }
        } else if (count == 49) {
            this.damage.normalDamage = 45;
            this.damage.sanDamage = 20;
        } else if (count == 50) {
            this.damage.normalDamage = 0;
            this.damage.sanDamage = 0;
        }

        return this.damage;
    }
}
