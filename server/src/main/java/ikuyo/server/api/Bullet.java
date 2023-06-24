package ikuyo.server.api;

import ikuyo.api.datatypes.Damage;
import ikuyo.api.datatypes.Drawable;
import ikuyo.server.behaviors.UserAttackBehavior;
import ikuyo.utils.MsgDiffer;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Ray;
import org.dyn4j.geometry.Vector2;


public class Bullet {
    public KitasBody body = new KitasBody();
    public String type;
    public Damage damage;
    public Drawable.Sprite drawable;
    public long frame = 0;
    public boolean ifAddBodyToWorld = true;

    public void set(UserAttackBehavior.BulletInfo info, CommonContext context) {
        this.type = info.type;
        this.damage = info.damage;
        BodyFixture fixture = body.addFixture(Geometry.createCircle(info.r));
        fixture.setFriction(0);
        fixture.setFilter(PhysicsEngine.BULLET);

        body.translate(info.pos.x, info.pos.y);
        body.setLinearVelocity(info.velocity);
        body.setRotatable(true);
        body.setBearTheGravity(true);
        body.setMass(MassType.NORMAL);
        body.setLinearDamping(0.01);
        body.setUserData(info.userId);
        body.setGravityScale(info.gravityScale);

        this.drawable = new Drawable.Sprite();
        this.updateDrawable();
        drawable.bundle = "bullet";
        drawable.asset = this.type;
        drawable.zIndex = 3;
        this.setFrame(info.liveTime);
    }

    public KitasBody getBody() {
        return body;
    }

    public void updateDrawable() {
        if (frame != 0) {
            Vector2 pos = this.body.getWorldCenter();
            drawable.x = pos.x * Drawable.scaling;
            drawable.y = pos.y * Drawable.scaling;
        }
    }

    public Drawable getDrawable() {
        return this.drawable;
    }

    public void setFrame(long frame) {
        this.frame = frame;
    }

    public void frame() {
        if (frame != 0) {
            frame--;
        }
        else {
            this.drawable = null;
        }
    }

    public void update(CommonContext context) {}

    public static class Laser extends Bullet {
        public Drawable.Line drawable;
        public Vector2 start;
        public Vector2 end;
        @Override
        public void set(UserAttackBehavior.BulletInfo info, CommonContext context) {
            this.ifAddBodyToWorld = false;

            body = info.userBody;
            this.type = info.type;
            this.damage = info.damage;
            this.drawable = new Drawable.Line();
            drawable.zIndex = 3;
            drawable.width = 3;
            drawable.color = 16764928;

            this.setFrame(60);
            this.update(context);
            this.updateDrawable();
        }

        @Override
        public void updateDrawable() {
            if (frame != 0) {
                drawable.x = start.x * Drawable.scaling;
                drawable.y = start.y * Drawable.scaling;

                drawable.lineTo(end.x * Drawable.scaling, end.y * Drawable.scaling);
                drawable.length = MsgDiffer.cacheRange;
                drawable.width = (60 - frame) / 60.0 * 7 + 3;
            }
        }

        @Override
        public Drawable getDrawable() {
            return this.drawable;
        }

        @Override
        public void update(CommonContext context) {
            Vector2  userPos = this.body.getWorldCenter();
            this.end = context.getState((int)this.body.getUserData()).input.pointAt.toVector();
            Vector2 direction = new Vector2(end.x - userPos.x, end.y - userPos.y).getNormalized();
            this.start = direction.copy().multiply(this.body.getRotationDiscRadius()).add(userPos);
            if (end.copy().subtract(userPos).getMagnitude() < start.copy().subtract(userPos).getMagnitude())
                this.end = this.start.copy().add(direction);

            var rayCast = context.engine().rayCast(new Ray(userPos, direction),
                            this.body.getRotationDiscRadius(), filter -> filter.equals(PhysicsEngine.BLOCK));
            rayCast.ifPresent(kitasBodyBodyFixtureRaycastResult ->
                    this.start = kitasBodyBodyFixtureRaycastResult.copy().getRaycast().getPoint());
        }
    }
}


