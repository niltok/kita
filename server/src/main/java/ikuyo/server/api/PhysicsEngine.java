package ikuyo.server.api;

import ikuyo.api.*;
import ikuyo.server.UpdateVert;
import org.dyn4j.collision.Filter;
import org.dyn4j.collision.Fixture;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.Force;
import org.dyn4j.dynamics.Settings;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PhysicsEngine{
    protected World<Body> world;
    public Map<Integer, Body> users;
    public Map<Integer, Body> surfaceBlocks;
    private static final Vector2 Gravity = new Vector2(-1000, 0);
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

        users = new HashMap<>();
        surfaceBlocks = new HashMap<>();
    }
    public void Initialize(Star star) {

        for (var b: StarInfo.SurfaceBlocks(0, 0, star.starInfo().maxTier, star.starInfo())) {
            if (star.starInfo().blocks[b].isCollidable) {
                Body body = new Body();
                Fixture fixture = body.addFixture(Geometry.createCircle(0.5));

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
            for(var body: world.getBodies()) {
                body.applyForce(Gravity.inverseRotate
                        (Math.atan2(body.getWorldCenter().x, body.getWorldCenter().y)));
            }
            world.step(1);
        }
    }

    public void addUser(User user, StarInfo.StarUserInfo userInfo) {
        if (!users.containsKey(user.id())) {
            Body body = new Body();
            Fixture fixture = body.addFixture(Geometry.createCircle(2.5));
            fixture.setFilter(userFilter);
            if (user.isAdmin()) fixture.setFilter(filter -> false);
            body.translate(userInfo.x, userInfo.y);
            body.setMass(MassType.NORMAL);
            users.put(user.id(), body);
            world.addBody(body);
        }
//        todo: Add check
//        System.out.println("!!!!!ADD BODY!!!!! [userid]: %d, [BODY]:%b".formatted(user.getKey(), users.get(user.getKey()) != null));
//        System.out.println("[x]: %f, [y]: %f".formatted(users.get(user.getKey()).getWorldCenter().x, users.get(user.getKey()).getWorldCenter().y));
    }

    public void removeUser(int id) {
        if (users.containsKey(id)) {
            Body body = users.get(id);
            world.removeBody(body);
            users.remove(id);
        }
    }

    public void addForce(Body body, Force force) {
        body.applyForce(force);
    }

    public void main(String[] args) {

    }
}
