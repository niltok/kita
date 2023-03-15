package ikuyo.server.api;

import ikuyo.api.Position;
import ikuyo.api.Star;
import ikuyo.api.StarInfo;
import ikuyo.api.User;
import ikuyo.server.UpdateVert;
import org.dyn4j.collision.Filter;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.dynamics.Force;
import org.dyn4j.dynamics.Settings;
import org.dyn4j.geometry.*;
import org.dyn4j.world.PhysicsWorld;
import org.dyn4j.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PhysicsEngine{
    protected World<Body> world;
    public Map<Integer, Map.Entry<User, Body>> users;
    public Map<Integer, Body> surfaceBlocks;
    public Map<String, Body> bullets;
    /**重力加速度*/
    public static final Vector2 Gravity = new Vector2(-1000, 0);
    private static final Polygon hexagon = Geometry
            .createPolygon(getVertices());
    private final Filter userFilter = new Filter() {
        @Override
        public boolean isAllowed(Filter filter) {
            return !filter.equals(this) && !filter.equals(bulletFilter);
        }
    };
    private final Filter bulletFilter = filter -> !filter.equals(userFilter);
    public PhysicsEngine() {
        world = new World<>();
        Settings settings = new Settings();
        settings.setStepFrequency(1 / UpdateVert.MaxFps);
        world.setSettings(settings);
        world.setGravity(PhysicsWorld.ZERO_GRAVITY);

        users = new HashMap<>();
        surfaceBlocks = new HashMap<>();
        bullets = new HashMap<>();
    }
    public void Initialize(Star star) {

//        表面块 body 创建
        for (int i = 0; i < star.starInfo().blocks.length; i++) {
            if (star.starInfo().blocks[i].isSurface && star.starInfo().blocks[i].isCollisible) {
                Body body = new Body();
                BodyFixture fixture = body.addFixture(hexagon);
                fixture.setFriction(0.1);
                Position pos = StarInfo.posOf(StarInfo.realIndexOf(i, star.starInfo().minTier));
                body.translate(pos.x, pos.y);
                body.setMass(MassType.INFINITE);
                surfaceBlocks.put(i, body);
                world.addBody(body);
            }
        }
    }
    public void EngineStep(int step) {
        for (int i = 0; i < step; i++) {
//            Gravity
            for(var body: world.getBodies()) {
                body.applyForce(new Vector2(Gravity.x * body.getMass().getMass(), Gravity.y)
                        .rotate(Math.atan2(body.getWorldCenter().y, body.getWorldCenter().x)));
            }

            for (var user: users.entrySet()) {
                if (user.getValue().getKey().isAdmin()) {
                    Body body = user.getValue().getValue();
                    body.applyForce(new Vector2(-Gravity.x * body.getMass().getMass(), -Gravity.y)
                            .rotate(Math.atan2(body.getWorldCenter().y, body.getWorldCenter().x)));
                }
            }

            world.step(1);
        }
    }

    public void addUser(User user, StarInfo.StarUserInfo userInfo) {
        if (!users.containsKey(user.id())) {
            Body body = new Body();
            BodyFixture fixture = body.addFixture(Geometry.createRectangle(5, 5));
            fixture.setFriction(0.1);
            fixture.setFilter(userFilter);
            body.translate(userInfo.x, userInfo.y);

//            {Circle} [double]: mass * r2 * 0.5
//            {Rectangle} [inertia]: mass * (height * height + width * width) / 12.0;
            body.setMass(new Mass(new Vector2(), 50,
                    (double) 50 * 50 / 12));
            body.setLinearDamping(1);

            if (user.isAdmin()) {
                fixture.setFilter(filter -> false);
                body.setLinearDamping(10);
            }

            body.setAtRest(true);
            users.put(user.id(), Map.entry(user, body));
            world.addBody(body);
        }
//        System.out.println("!!!!!ADD BODY!!!!! [userid]: %d, [BODY]:%b".formatted(user.getKey(), users.get(user.getKey()) != null));
//        System.out.println("[x]: %f, [y]: %f".formatted(users.get(user.getKey()).getWorldCenter().x, users.get(user.getKey()).getWorldCenter().y));
    }

    public void removeUser(int id) {
        if (users.containsKey(id)) {
            Body body = users.get(id).getValue();
            world.removeBody(body);
            users.remove(id);
        }
    }

    public Body addBullet(Position pos) {
        Body body = new Body();
        BodyFixture fixture = body.addFixture(Geometry.createCircle(0.3));
        fixture.setFriction(0.1);
        fixture.setFilter(bulletFilter);
        body.translate(pos.x, pos.y);
        body.setMass(MassType.NORMAL);
        bullets.put(UUID.randomUUID().toString(), body);
        world.addBody(body);

        return body;
    }

    public void addForce(Body body, Force force) {
        body.applyForce(force);
    }

    private static Vector2[] getVertices() {
        Vector2[] vertices = new Vector2[6];
        for (int i = 0; i < 6; i++) {
            vertices[i] = new Vector2(1 / Math.pow(3, 1 / 2.0), 0).rotate(Math.PI / 3 * i);
        }
        return vertices;
    }

    public static void main(String[] args) {
//        System.out.println(new Vector2().y);
    }
}
