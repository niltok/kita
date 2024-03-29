package ikuyo.server.api;

import ikuyo.api.entities.User;
import org.dyn4j.geometry.Ray;
import org.dyn4j.geometry.Transform;
import org.dyn4j.geometry.Vector2;

public class UserEngineData {
    public User user;
    public KitasBody body = new KitasBody();
    public Camera camera = new Camera();

    public double groundClearance;

    public UserEngineData(User user) {
        this.user = user;
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

    public void preprocess(PhysicsEngine engine) {
        Vector2 info = body.getWorldCenter();
        this.groundClearance = engine.rayCast(
                        new Ray(new Vector2(info.x, info.y), new Vector2(-info.x, -info.y)),
                        Math.hypot(info.x, info.y), filter -> filter.equals(PhysicsEngine.BLOCK))
                .map(res -> res.getRaycast().getDistance()).orElse(Double.NaN);

        Vector2 pointer = body.getWorldCenter().subtract(camera.getWorldCenter());
        if (camera.getIfFollowBody() && pointer.getMagnitude() < 0.001)
            camera.setLinearVelocity(body.getLinearVelocity());
//        todo: camera pos
         else camera.setLinearVelocity(pointer.getNormalized().multiply(Math.pow(pointer.getMagnitude(), 3)));
//        else camera.setLinearVelocity(camera.getLinearVelocity()
//                .add(pointer.getNormalized().multiply(Math.pow(pointer.getMagnitude(), 2))));
//        else this.camera.applyForce(pointer.getNormalized().multiply(Math.pow(pointer.getMagnitude() * 100, 5)));
    }

    public UserEngineData reSetCamera() {
        camera.getTransform().setTranslation(body.getTransform().getTranslation());
        camera.setLinearVelocity(body.getLinearVelocity());

        return this;
    }

    public UserEngineData cameraMove() {
        return this;
    }
}
