package ikuyo.server.api;

import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.MassType;

public class Camera extends KitasBody {
    private boolean followBody = true;

    public Camera() {
        this.setBearTheGravity(false);
        this.setGravityScale(0);
        this.setAngularDamping(0);
        this.setMass(MassType.NORMAL);
        BodyFixture fixture = this.addFixture(Geometry.createCircle(0.01));
        fixture.setFilter(filter -> false);
    }

    public boolean getIfFollowBody() {
        return followBody;
    }

    public void setIfFollowBody(boolean state) {
        followBody = state;
    }
}
