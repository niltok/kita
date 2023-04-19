package ikuyo.server.api;

import ikuyo.api.entities.User;
import org.dyn4j.geometry.Ray;
import org.dyn4j.geometry.Transform;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.result.RaycastResult;

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
        var minHit = engine.rayCast(
                        new Ray(new Vector2(info.x, info.y), new Vector2(-info.x, -info.y)),
                        Math.hypot(info.x, info.y), filter -> filter.equals(PhysicsEngine.BLOCK))
                .stream().min(RaycastResult::compareTo);
        this.groundClearance = minHit.map(res -> res.getRaycast().getDistance()).orElse(Double.NaN);

        Vector2 pointer = body.getWorldCenter().subtract(camera.getWorldCenter());
        if (camera.getIfFollowBody() && pointer.getMagnitude() < 0.001)
            camera.setLinearVelocity(body.getLinearVelocity());
//        todo: camera pos
//        else camera.setLinearVelocity(pointer.getNormalized().multiply(Math.pow(pointer.getMagnitude(), 10)));
        else this.setCameraPosition(body.getWorldCenter().x, body.getWorldCenter().y);
    }
}
