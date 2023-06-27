package ikuyo.server.api;

import ikuyo.api.datatypes.StarInfo;
import ikuyo.api.datatypes.UserInfo;
import ikuyo.api.entities.Star;
import ikuyo.api.entities.User;
import ikuyo.server.UpdateVert;
import ikuyo.utils.Position;
import ikuyo.utils.StarUtils;
import org.dyn4j.collision.Filter;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.dynamics.Settings;
import org.dyn4j.geometry.*;
import org.dyn4j.world.CollisionData;
import org.dyn4j.world.DetectFilter;
import org.dyn4j.world.PhysicsWorld;
import org.dyn4j.world.World;
import org.dyn4j.world.result.DetectResult;
import org.dyn4j.world.result.RaycastResult;

import java.util.*;
import java.util.stream.Stream;

public class PhysicsEngine {
    protected World<KitasBody> dynamicWorld;
    protected World<KitasBody> staticWorld;
//    public Map<Integer, Map.Entry<User, KitasBody>> users;

    public Map<Integer, UserEngineData> users;
    public Map<Integer, KitasBody> surfaceBlocks;
    public Map<String, Bullet> bullets;
    /**重力加速度*/
    public static final double GravitationalAcc = 300;
    private static final Polygon hexagon = Geometry.createPolygon(getVertices());
    public static final Filter BULLET = filter -> true;
    public static final Filter BLOCK = filter -> true;
    public static final double starR = StarInfo.maxTier + 1;
    public static final Filter USER = new Filter() {
        @Override
        public boolean isAllowed(Filter filter) {
            return !filter.equals(this);
        }
    };

    public PhysicsEngine() {
        dynamicWorld = new World<>();
        staticWorld = new World<>();
        Settings settings = new Settings();
        settings.setStepFrequency(1 / UpdateVert.MaxFps);
        settings.setMaximumTranslation(100);
        dynamicWorld.setSettings(settings);
        dynamicWorld.setGravity(PhysicsWorld.ZERO_GRAVITY);

        users = new HashMap<>();
        surfaceBlocks = new HashMap<>();
        bullets = new HashMap<>();
    }

    public int dynBodyCount() {
        return users.size() + bullets.size();
    }

    public int bodyCount() {
        return dynamicWorld.getBodyCount();
    }

    public void Initialize(Star star) {
//        表面块 body 创建
        for (int i = 0; i < star.starInfo().blocks.length; i++) {
            if (star.starInfo().blocks[i].isSurface && star.starInfo().blocks[i].isCollisible) {
                addBlock(i);
                if (AreaState.workSet) {
                    var block = surfaceBlocks.get(i);
                    disableBody(block);
                }
            }
        }
    }

    public void EngineStep(int step) {
        for (int i = 0; i < step; i++) {
//            for (var body: world.getBodies())
//                body.preprocess();

            for (var bullet: bullets.values()) {
                bullet.body.preprocess();
                checkOutOfBound(bullet.getBody(), starR * 2);
            }

            for (var user: users.values()) {
                user.body.preprocess();
                checkOutOfBound(user.getBody(), starR * 2);
            }

            dynamicWorld.step(1);
        }
    }

    private void checkOutOfBound(KitasBody body, double bound) {
        Vector2 pos = body.getWorldCenter();
        if (pos.getMagnitude() >= bound) {
            body.clearForce();
            body.applyForce(pos.getNormalized().multiply(-1000 * (pos.getMagnitude() - bound)));
        }
    }

    public void addBody(KitasBody body) {
        dynamicWorld.addBody(body);
    }

    public void removeBody(KitasBody body) {
        dynamicWorld.removeBody(body);
    }

    public void addUser(User user, UserInfo userInfo) {
        if (!users.containsKey(user.id())) {
            UserEngineData userData = new UserEngineData(user);

            KitasBody body = userData.getBody();
            BodyFixture fixture = body.addFixture(Geometry.createRectangle(5, 5));
            bodyCreator(body, fixture, user.id());
            body.setFixRotation(true);
            body.translate(userInfo.x, userInfo.y);

//            body.setAngularDamping(Double.MAX_VALUE);

//            {Circle} [double]: mass * r2 * 0.5
//            {Rectangle} [inertia]: mass * (height * height + width * width) / 12.0;
            body.setMass(new Mass(new Vector2(), 50,
                    (double) 50 * 50 / 12));

            if (user.isAdmin()) {
                fixture.setFilter(filter -> false);
//                fixture.setFilter(USER);
                body.setLinearDamping(2);
                body.setGravityScale(0);
                body.setRotatable(true);
            }

            userData.setCameraPosition(userInfo.x, userInfo.y);

            body.setAtRest(true);
            users.put(user.id(), userData);
            dynamicWorld.addBody(body);
            dynamicWorld.addBody(userData.camera);

//            KitasBody frontWheel = new KitasBody();
//            bodyCreator(frontWheel, frontWheel.addFixture(Geometry.createCircle(0.5)), user.id());
//            frontWheel.translate(userInfo.x + 1.5, userInfo.y - 2.5);
//            dynamicWorld.addBody(frontWheel);
//            RevoluteJoint<KitasBody> frontJoint = new RevoluteJoint<>(body, frontWheel, new Vector2(userInfo.x + 1.5, userInfo.y - 2.5));
////            RevoluteJoint<KitasBody> frontJoint = new RevoluteJoint<>(body, frontWheel);
//            dynamicWorld.addJoint(frontJoint);
//
//            KitasBody rearWheel = new KitasBody();
//            bodyCreator(rearWheel, rearWheel.addFixture(Geometry.createCircle(0.5)), user.id());
//            rearWheel.translate(userInfo.x - 1.5, userInfo.y - 2.5);
//            dynamicWorld.addBody(rearWheel);
//            RevoluteJoint<KitasBody> rearJoint = new RevoluteJoint<>(body, rearWheel, new Vector2(userInfo.x - 1.5, userInfo.y - 2.5));
////            AngleJoint<KitasBody> rearJoint = new AngleJoint<>(body, rearWheel);
//            dynamicWorld.addJoint(rearJoint);
        }
    }

    private static void bodyCreator(KitasBody body, BodyFixture fixture, int userId) {
        fixture.setFriction(0.1);
        fixture.setFilter(USER);
        body.setRotatable(true);
        body.setBearTheGravity(true);
        body.setLinearDamping(1);
        body.setAngularDamping(5);
        body.setUserData(userId);
    }

    public void removeUser(int id) {
        if (users.containsKey(id)) {
            UserEngineData data = users.get(id);
            dynamicWorld.removeBody(data.getBody());
            dynamicWorld.removeBody(data.getCamera());
            users.remove(id);
        }
    }

    public void addBullet(Bullet bullet) {
        bullets.put(UUID.randomUUID().toString(), bullet);
        if (bullet.ifHasEntity)
            dynamicWorld.addBody(bullet.getBody());
    }

    public void removeBullet(String id) {
        if (bullets.get(id) != null && bullets.get(id).ifHasEntity)
            dynamicWorld.removeBody(bullets.get(id).getBody());
        bullets.put(id, null);
    }

    public void addBlock(int id) {
        if (surfaceBlocks.get(id) != null)
            dynamicWorld.removeBody(surfaceBlocks.get(id));
        KitasBody body = new KitasBody();
        BodyFixture fixture = body.addFixture(hexagon);
        fixture.setFriction(0.1);
        fixture.setFilter(BLOCK);
        Position pos = StarUtils.positionOf(StarUtils.realIndexOf(id));
        body.translate(pos.x, pos.y);
//        body.getTransform().setRotation(Math.atan2(pos.y, pos.x));
        body.setMass(MassType.INFINITE);
        surfaceBlocks.put(id, body);
        dynamicWorld.addBody(body);
    }

    public Iterator<DetectResult<KitasBody, BodyFixture>> broadPhaseDetect(KitasBody body, Filter filter) {
        return dynamicWorld.detectIterator(
                body.getFixture(0).getShape(),
                body.getTransform(),
                new DetectFilter(true, true, filter));
    }

    public Iterator<DetectResult<KitasBody, BodyFixture>> broadPhaseDetect(AABB aabb, Filter filter) {
        return dynamicWorld.detectIterator(aabb, new DetectFilter<>(true, true, filter));
    }

    public boolean ManifoldDetect(KitasBody body, Iterator<DetectResult<KitasBody, BodyFixture>> iterator) {
        while (iterator.hasNext()) {
            DetectResult<KitasBody, BodyFixture> result = iterator.next().copy();

            CollisionData<KitasBody, BodyFixture> data = dynamicWorld.getCollisionData(
                    body,
                    body.getFixture(0),
                    result.getBody(),
                    result.getFixture());

            if (data != null && data.isManifoldCollision()) return true;
        }
        return false;
    }

    public Optional<RaycastResult<KitasBody, BodyFixture>> rayCast(Ray ray, double length, Filter filter) {
        var result1 = dynamicWorld.raycast(ray,
                length,
                new DetectFilter<>(true, true, filter))
                .stream();
        var result2 = staticWorld.raycast(ray,
                length,
                new DetectFilter<>(true, true, filter))
                .stream();
        return Stream.concat(result1, result2).min(RaycastResult::compareTo);
    }

    private static Vector2[] getVertices() {
        Vector2[] vertices = new Vector2[6];
        for (int i = 0; i < 6; i++) {
            vertices[i] = new Vector2(StarInfo.edgeLength + 1e-9, 0).rotate(Math.PI / 3 * i + Math.PI / 6);
        }
        return vertices;
    }

    public void enableBody(KitasBody body) {
        staticWorld.removeBody(body);
        dynamicWorld.addBody(body);
    }
    public void disableBody(KitasBody body) {
        dynamicWorld.removeBody(body);
        staticWorld.addBody(body);
    }

    public void frame() {
        for (var bullet: this.bullets.values()) {
            bullet.frame();
        }
    }


    public static void main(String[] args) {
//        System.out.println(new Vector2().y);
    }
}
