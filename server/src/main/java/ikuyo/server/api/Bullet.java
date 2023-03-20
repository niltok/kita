package ikuyo.server.api;

import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Vector2;


public class Bullet {
    public KitasBody body;
    public String type;
    public double range = 0;
    public double damage = 0;

    public Bullet(Vector2 pos) {
        body = new KitasBody();
        BodyFixture fixture = body.addFixture(Geometry.createCircle(0.3));
        fixture.setFriction(0);
        fixture.setFilter(PhysicsEngine.BULLET);
        body.translate(pos.x, pos.y);
        body.setRotatable(true);
        body.setBearTheGravity(true);
        body.setMass(MassType.NORMAL);
        body.setLinearDamping(0.01);
    }

    public void set(String type, double range, double damage) {
        this.type = type;
        this.range = range;
        this.damage = damage;
    }
}
