package ikuyo.server.api;

import com.fasterxml.jackson.databind.node.POJONode;
import ikuyo.api.*;
import ikuyo.api.behaviors.Behavior;
import ikuyo.server.UpdateVert;
import ikuyo.server.api.BehaviorContext;
import ikuyo.utils.Enumerator;
import io.vertx.pgclient.PgPool;
import io.vertx.pgclient.impl.PgPoolImpl;
import io.vertx.sqlclient.PoolOptions;
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
    public World<Body> world;
    public Map<Integer, Body> users;
    public Map<Integer, Body> surfaceBlock;
    public PhysicsEngine(Star star) {
        world = new World<>();
        Settings settings = new Settings();
        settings.setStepFrequency(1 / UpdateVert.MaxFps);
        world.setSettings(settings);

        users = new HashMap<>();
        surfaceBlock = new HashMap<>();

        for (var b: StarInfo.SurfaceBlocks(0, star.starInfo().maxtier, star.starInfo())) {
            if (star.starInfo().blocks[b].isCollidable) {
                Body body = new Body();
                body.addFixture(Geometry.createCircle(0.5));
                Position pos = StarInfo.posOf(StarInfo.realIndexOf(b, star.starInfo().mintier));
                body.translate(pos.x, pos.y);
                body.setMass(MassType.INFINITE);
                surfaceBlock.put(b, body);
                world.addBody(body);
            }
        }

        for (var user: star.starInfo().starUsers.entrySet()) {
            addUser(user);
        }
    }

    public void addUser(Map.Entry<Integer, StarInfo.StarUserInfo> user) {
        Body body = new Body();
        body.addFixture(Geometry.createCircle(1.0));
        body.translate(user.getValue().x, user.getValue().y);
        body.setMass(MassType.NORMAL);
        users.put(user.getKey(), body);
        world.addBody(body);
//        System.out.println("!!!!!ADD BODY!!!!! [userid]: %d, [BODY]:%b".formatted(user.getKey(), users.get(user.getKey()) != null));
//        System.out.println("[x]: %f, [y]: %f".formatted(users.get(user.getKey()).getWorldCenter().x, users.get(user.getKey()).getWorldCenter().y));
    }

    public void addForce(Body body, Force force) {
        body.applyForce(force);
    }

    public void main(String[] args) {
//        PhysicsEngine engine = new PhysicsEngine(star);
//        world = new World<Body>();
//        for (var b: star.blocks) {
//            if (b.isCollidable) {
//                Body body = new Body();
//                body.addFixture(Geometry.createCircle(1.0));
//                body.translate(1.0, 0.0);
//                body.setMass(MassType.NORMAL);
//                Force force = new Force();
//                body.getForce();
//                world.addBody(body);
//                world.getBody(0);
//                world.setGravity(0,0);
//            }
//        }
    }
}
