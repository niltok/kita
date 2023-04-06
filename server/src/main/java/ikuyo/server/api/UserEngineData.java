package ikuyo.server.api;

import ikuyo.api.entities.User;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.Transform;

public class UserEngineData {
    public User user;
    public KitasBody body = new KitasBody();
    public KitasBody camera;

    public UserEngineData() {
        camera = new KitasBody();
        camera = new KitasBody();
        camera.setBearTheGravity(false);
        camera.setGravityScale(0);
        BodyFixture fixture = camera.addFixture(Geometry.createCircle(0.01));
        fixture.setFilter(filter -> false);
    }

    public KitasBody getBody() {
        return body;
    }

    public KitasBody getCamera() {
        return camera;
    }

    public void setCameraPosition(double x, double y) {
        Transform t = new Transform();
        t.setTranslation(x, y);
        this.camera.setTransform(t);
    }
}
