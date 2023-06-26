package ikuyo.server.api;

import ikuyo.api.datatypes.Damage;
import ikuyo.api.datatypes.Drawable;
import ikuyo.server.behaviors.UserAttackBehavior;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Vector2;


public class Bullet {
    public KitasBody body = new KitasBody();
    public String type;
    public Damage damage;
    public Drawable.Sprite drawable;
    public long frame = 0;
    public boolean ifHasEntity = true;

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
        this.setFrame(info.liveTime);
        this.updateDrawable();
        drawable.bundle = "bullet";
        drawable.asset = this.type;
        drawable.zIndex = 3;

    }

    public KitasBody getBody() {
        return body;
    }

    public void updateDrawable() {
        if (frame == 0) {
            this.drawable = null;
            return;
        }
        Vector2 pos = this.body.getWorldCenter();
        drawable.x = pos.x * Drawable.scaling;
        drawable.y = pos.y * Drawable.scaling;
    }

    public Drawable getDrawable() {
        return this.drawable;
    }

    public void setFrame(long frame) {
        this.frame = frame;
    }

    public void frame() {
        if (frame != 0)
            frame--;
    }

    public void update(CommonContext context) {}

}


