package ikuyo.server.api;

import ikuyo.server.behaviors.UserAttackBehavior;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.MassType;


public class Bullet {
    public KitasBody body = new KitasBody();
    public String type;
    public double range = 0;
    public double damage = 0;

    public void set(UserAttackBehavior.BulletInfo info) {
        this.type = info.type;
        this.range = info.range;
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
    }
}
