package ikuyo.server.api;

import ikuyo.api.Position;
import ikuyo.api.Star;
import ikuyo.api.StarInfo;
import ikuyo.api.User;
import ikuyo.server.UpdateVert;
import org.dyn4j.collision.Filter;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.dynamics.Settings;
import org.dyn4j.geometry.*;
import org.dyn4j.world.CollisionData;
import org.dyn4j.world.DetectFilter;
import org.dyn4j.world.PhysicsWorld;
import org.dyn4j.world.World;
import org.dyn4j.world.result.DetectResult;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class PhysicsEngine{
    protected World<KitasBody> world;
    public Map<Integer, Map.Entry<User, KitasBody>> users;
    public Map<Integer, KitasBody> surfaceBlocks;
    public Map<String, Bullet> bullets;
    /**重力加速度*/
    public static final double GravitationalAcc = 300;
    private static final Polygon hexagon = Geometry.createPolygon(getVertices());
    public static final Filter BULLET = filter -> true;
    public static final Filter BLOCK = filter -> true;
    public static final Filter USER = new Filter() {
        @Override
        public boolean isAllowed(Filter filter) {
            return !filter.equals(this);
        }
    };

    public PhysicsEngine() {
        world = new World<>();
        Settings settings = new Settings();
        settings.setStepFrequency(1 / UpdateVert.MaxFps);
        settings.setMaximumTranslation(100);
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
                addBlock(i);
            }
        }
    }

    public void EngineStep(int step) {
        for (int i = 0; i < step; i++) {
            for (var body: world.getBodies()) {
                body.setBearTheGravity(!body.getBearTheGravity()
                        || !(body.getWorldCenter().distance(0, 0) >= starR));
                body.applyGravity();
                body.updateRotation();
            }

/*            List<String> removeList = new ArrayList<>();
            for (var entry: bullets.entrySet()) {
                if (entry.getValue() == null)
                    continue;
                if (entry.getValue().body.getWorldCenter().distance(0, 0) >= starR * 2)
                    removeList.add(entry.getKey());
            }
            for (var id: removeList)
                removeBullet(id);*/

            for (var bullet: bullets.values())
                checkOutOfBound(bullet.body, starR * 2);

            for (var user: users.values())
                checkOutOfBound(user.getValue(), starR * 2);

            world.step(1);
        }
    }

    private void checkOutOfBound(KitasBody body, double bound) {
        Vector2 pos = body.getWorldCenter();
        if (pos.getMagnitude() >= bound) {
            body.clearForce();
            body.applyForce(pos.getNormalized().multiply(-1000 * (pos.getMagnitude() - bound)));
        }
    }

    private static final double starR = StarInfo.maxTier + 1;
    private void applyGravity(KitasBody body) {
        Vector2 pos = body.getWorldCenter();
        if (pos.distance(0, 0) <= starR)
            body.applyForce(new Vector2(-GravitationalAcc * body.getMass().getMass(), 0)
                    .rotate(Math.atan2(pos.y, pos.x)));
    }

    public void removeBody(KitasBody body) {
        world.removeBody(body);
    }

    public void addUser(User user, StarInfo.StarUserInfo userInfo) {
        if (!users.containsKey(user.id())) {
            KitasBody body = new KitasBody();
            BodyFixture fixture = body.addFixture(Geometry.createRectangle(5, 5));
            fixture.setFriction(0.1);
            fixture.setFilter(USER);
            body.translate(userInfo.x, userInfo.y);
            body.setRotatable(true);
            body.setBearTheGravity(true);
            body.setFixRotation(true);
            body.setLinearDamping(1);
            body.setAngularDamping(5);
            body.setUserData(user.id());
//            body.setAngularDamping(Double.MAX_VALUE);

//            {Circle} [double]: mass * r2 * 0.5
//            {Rectangle} [inertia]: mass * (height * height + width * width) / 12.0;
            body.setMass(new Mass(new Vector2(), 50,
                    (double) 50 * 50 / 12));

            if (user.isAdmin()) {
//                fixture.setFilter(filter -> false);
                fixture.setFilter(USER);
                body.setLinearDamping(5);
                body.setGravityScale(0);
                body.setRotatable(true);
            }

            body.setAtRest(true);
            users.put(user.id(), Map.entry(user, body));
            world.addBody(body);
        }
    }

    public void removeUser(int id) {
        if (users.containsKey(id)) {
            KitasBody body = users.get(id).getValue();
            world.removeBody(body);
            users.remove(id);
        }
    }

    public Bullet addBullet(Vector2 pos) {
        Bullet bullet = new Bullet(pos);
        bullets.put(UUID.randomUUID().toString(), bullet);
        world.addBody(bullet.body);
        return bullet;
    }

    public void removeBullet(String id) {
        world.removeBody(bullets.get(id).body);
        bullets.put(id, null);
    }

    public void addBlock(int id) {
        if (surfaceBlocks.get(id) != null)
            world.removeBody(surfaceBlocks.get(id));
        KitasBody body = new KitasBody();
        BodyFixture fixture = body.addFixture(hexagon);
        fixture.setFriction(0.1);
        fixture.setFilter(BLOCK);
        Position pos = StarInfo.posOf(StarInfo.realIndexOf(id, StarInfo.minTier));
        body.translate(pos.x, pos.y);
        body.getTransform().setRotation(Math.atan2(pos.y, pos.x));
        body.setMass(MassType.INFINITE);
        surfaceBlocks.put(id, body);
        world.addBody(body);
    }

    public Iterator<DetectResult<KitasBody, BodyFixture>> broadPhaseDetect(KitasBody body, Filter filter) {
        return world.detectIterator(
                body.getFixture(0).getShape(),
                body.getTransform(),
                new DetectFilter(true, true, filter));
    }

    public Iterator<DetectResult<KitasBody, BodyFixture>> broadPhaseDetect(AABB aabb, Filter filter) {
        return world.detectIterator(aabb, new DetectFilter(true, true, filter));
    }

    public boolean ManifoldDetect(KitasBody body, Iterator<DetectResult<KitasBody, BodyFixture>> iterator) {
        while (iterator.hasNext()) {
            DetectResult<KitasBody, BodyFixture> result = iterator.next().copy();

            CollisionData<KitasBody, BodyFixture> data = world.getCollisionData(
                    body,
                    body.getFixture(0),
                    result.getBody(),
                    result.getFixture());

            if (data != null && data.isManifoldCollision()) return true;
        }
        return false;
    }

    private static Vector2[] getVertices() {
        Vector2[] vertices = new Vector2[6];
        for (int i = 0; i < 6; i++) {
            vertices[i] = new Vector2(StarInfo.edgeLength, 0).rotate(Math.PI / 3 * i + Math.PI / 6);
        }
        return vertices;
    }

    public static void main(String[] args) {
//        System.out.println(new Vector2().y);
    }
}
