package ikuyo.server.api;

import ikuyo.api.Position;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.MassType;


public class Bullet {
    public KitasBody body;
    public String type;

    public Bullet(String type, Position pos) {
        this.type = type;
        body = new KitasBody();
        BodyFixture fixture = body.addFixture(Geometry.createCircle(0.3));
        fixture.setFriction(0);
        fixture.setFilter(PhysicsEngine.bulletFilter);
        body.translate(pos.x, pos.y);
        body.setRotatable(true);
        body.setBearTheGravity(true);
        body.setMass(MassType.NORMAL);
        body.setLinearDamping(0);
    }
}
