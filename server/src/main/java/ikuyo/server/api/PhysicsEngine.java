package ikuyo.server.api;

import ikuyo.api.*;
import ikuyo.server.UpdateVert;
import org.dyn4j.collision.Filter;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.dynamics.Force;
import org.dyn4j.dynamics.Settings;
import org.dyn4j.geometry.*;
import org.dyn4j.world.PhysicsWorld;
import org.dyn4j.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class PhysicsEngine{
    protected World<Body> world;
    public Map<Integer, Map.Entry<User, Body>> users;
    public Map<Integer, Body> surfaceBlocks;
    protected static final Polygon hexagon = Geometry
            .createPolygon(getVertices());
    public static final Vector2 Gravity = new Vector2(-1000, 0);
    protected Filter userFilter = new Filter() {
        @Override
        public boolean isAllowed(Filter filter) {
            return !filter.equals(this);
        }
    };
    public PhysicsEngine() {
        world = new World<>();
        Settings settings = new Settings();
        settings.setStepFrequency(1 / UpdateVert.MaxFps);
        world.setSettings(settings);
        world.setGravity(PhysicsWorld.ZERO_GRAVITY);

        users = new HashMap<>();
        surfaceBlocks = new HashMap<>();
    }
    public void Initialize(Star star) {

        for (var b: StarInfo.SurfaceBlocks(0, 0, star.starInfo().maxTier, star.starInfo())) {
            if (star.starInfo().blocks[b].isCollidable) {
                Body body = new Body();
                BodyFixture fixture = body.addFixture(hexagon);
                fixture.setFriction(0.1);
                Position pos = StarInfo.posOf(StarInfo.realIndexOf(b, star.starInfo().minTier));
                body.translate(pos.x, pos.y);
                body.setMass(MassType.INFINITE);
                surfaceBlocks.put(b, body);
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
            if (user.isAdmin()) fixture.setFilter(filter -> false);
            body.translate(userInfo.x, userInfo.y);

//            {Circle} [double]: mass * r2 * 0.5
//            {Rectangle} [inertia]: mass * (height * height + width * width) / 12.0;
            body.setMass(new Mass(new Vector2(), 50,
                    (double) 50 * 50 / 12));
            body.setLinearDamping(1);
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
