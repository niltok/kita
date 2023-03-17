package ikuyo.server.api;

import ikuyo.api.Position;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.MassType;


public class Bullet {
    public Body body;
    public int type;

    public Bullet(int type, Position pos) {
        this.type = type;
        body = new Body();
        BodyFixture fixture = body.addFixture(Geometry.createCircle(0.3));
        fixture.setFriction(0);
        fixture.setFilter(PhysicsEngine.bulletFilter);
        body.translate(pos.x, pos.y);
        body.setMass(MassType.NORMAL);
        body.setLinearDamping(0);
    }
}
