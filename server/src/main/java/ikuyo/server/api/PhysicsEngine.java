package ikuyo.server.api;

import ikuyo.api.*;
import ikuyo.server.UpdateVert;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.Force;
import org.dyn4j.dynamics.Settings;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.MassType;
import org.dyn4j.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PhysicsEngine{
    public World<Body> world;
    public Map<Integer, Body> users;
    public Map<Integer, Body> surfaceBlocks;
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
                body.addFixture(Geometry.createCircle(0.5));
                Position pos = StarInfo.posOf(StarInfo.realIndexOf(b, star.starInfo().minTier));
                body.translate(pos.x, pos.y);
                body.setMass(MassType.INFINITE);
                surfaceBlocks.put(b, body);
                world.addBody(body);
            }
        }

        for (var user: star.starInfo().starUsers.entrySet()) {
            addUser(user);
        }
    }

    public void addUser(Map.Entry<Integer, StarInfo.StarUserInfo> user) {
        if (!users.containsKey(user.getKey())) {
            Body body = new Body();
            body.addFixture(Geometry.createCircle(2.5));
            body.translate(user.getValue().x, user.getValue().y);
            body.setMass(MassType.NORMAL);
            users.put(user.getKey(), body);
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
