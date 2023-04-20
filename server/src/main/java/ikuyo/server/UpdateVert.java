package ikuyo.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ikuyo.api.behaviors.CompositeBehavior;
import ikuyo.api.cargo.CargoStatic;
import ikuyo.api.datatypes.StarInfo;
import ikuyo.api.datatypes.UserInfo;
import ikuyo.api.entities.Star;
import ikuyo.api.entities.User;
import ikuyo.api.equipments.Weapon;
import ikuyo.api.renderers.CompositeRenderer;
import ikuyo.api.renderers.Renderer;
import ikuyo.api.renderers.UIRenderer;
import ikuyo.server.api.CommonContext;
import ikuyo.server.behaviors.*;
import ikuyo.server.renderers.*;
import ikuyo.utils.*;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Tuple;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class UpdateVert extends AsyncVerticle {
    public static final double MaxFps = 80;
    Star star;
    boolean loaded = false;
    MessageConsumer<JsonObject> vertEvents;
    long writeBackId, mainLoopId,
            updateTime, prevTime, deltaTime, startTime, msgHandleTime = 0;
    String msgVertId;
    PgPool pool;
    CompositeBehavior<CommonContext> mainBehavior = new CompositeBehavior<>(
            new KeyInputBehavior(),
            new ControlMovingBehavior(),
            new PhysicsEngineBehavior(),
            new PointerMovingBehavior(),
            new WeaponBehavior(),
            new UserAttackBehavior(),
            new BulletBehavior(),
            new PageBehavior(),
            new AreaBehavior()
    );
    DrawablesRenderer.Composite drawableRenderer = new DrawablesRenderer.Composite(
            new BlockRenderer(),
            new UserRenderer(),
            new BulletRenderer()
    );
    Renderer<CommonContext> commonSeqRenderer = new CompositeRenderer<>(false,
            drawableRenderer
    );
    Renderer<CommonContext> commonRenderer = new CompositeRenderer<>(true);
    UIRenderer.Composite<CommonContext> uiRenderer = new UIRenderer.Composite<>(
            new UserStateRenderer(),
            new CargoRenderer(),
            new AdminPanelRenderer()
    );
    Renderer<CommonContext> specialRenderer = new CompositeRenderer<>(true,
            new CameraRenderer().withName("camera"),
            uiRenderer.withName("ui")
    ).withName("star");
    CommonContext commonContext;

    @Override
    public void start() {
        pool = PgPool.pool(vertx, new PoolOptions());
        loadStar(config().getInteger("id"));
    }

    @Override
    public void stop() throws Exception {
        vertx.cancelTimer(writeBackId);
        vertx.cancelTimer(mainLoopId);
        await(CompositeFuture.all(
                vertEvents.unregister(),
                vertx.undeploy(msgVertId)));
        stopPool();
        logger.info(JsonObject.of(
                "type", "updater.undeploy",
                "starId", star.index(),
                "starName", star.name()));
    }

    private void stopPool() {
        await(tryWriteBack());
        try {
            await(pool.preparedQuery("update star set vert_id = null where vert_id = $1;")
                    .execute(Tuple.of(deploymentID())));
        } catch (Exception ignored) {}
        await(pool.close());
    }

    private void loadStar(int id) {
        // 乐观锁
        var locked = await(pool.preparedQuery("""
        update star
        set vert_id = $2, time_lock = now() + interval '15 seconds'
        where index = $1 and vert_id is null"""
        ).execute(Tuple.of(id, deploymentID()))).rowCount() == 1;
        if (!locked) {
            vertx.undeploy(deploymentID());
            return;
        }
        logger.info(JsonObject.of("type", "star.ownership.lock", "starId", id));
        vertEvents = eventBus.localConsumer(deploymentID(), v -> {
            var startTime = System.nanoTime();
            try {
                vertEventsHandler(v);
            } catch (Exception e) {
                logger.warn(v.body());
                logger.error(e.getLocalizedMessage());
                e.printStackTrace();
            } finally {
                msgHandleTime += System.nanoTime() - startTime;
            }
        });
        msgVertId = await(vertx.deployVerticle(MessageVert.class, new DeploymentOptions()
                .setWorker(true)
                .setConfig(JsonObject.of("updaterId", deploymentID(), "starId", id))));
        try {
            star = Star.get(pool, id);
        } catch (NullPointerException e) {
            star = Star.getSummery(pool, id);
            assert star != null;
            logger.info(JsonObject.of("type", "star.generating", "id", id, "name", star.name()));
            Buffer starInfo = await(Vertx.currentContext().executeBlocking(p ->
                    p.complete(StarInfo.generate(star.seed()).toBuffer()), false));
            await(pool.preparedQuery("""
                update star set star_info = $2 where index = $1 returning index
                """).execute(Tuple.of(id, starInfo)));
            star = Star.get(pool, id);
            assert star != null;
            logger.info(JsonObject.of("type", "star.generated", "id", id, "name", star.name()));
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());
            e.printStackTrace();
            vertx.undeploy(deploymentID());
            return;
        }
        commonContext = new CommonContext(vertx, star);
        startTime = System.nanoTime();
        prevTime = startTime;
        mainLoopId = vertx.setTimer(1, v -> mainLoop());
        writeBackId = vertx.setPeriodic(1 * 60 * 1000, ignore -> writeBack());
        logger.info(JsonObject.of("type", "star.loaded", "id", id, "name", star.name()));
        loaded = true;
    }

    private void vertEventsHandler(Message<JsonObject> msg) {
        while (!loaded) {
            if (!healthCheck()) return;
            doEvents();
        }
        var json = msg.body();
        if (enableMsgLog) logger.info(json);
        switch (json.getString("type")) {
            case "vert.undeploy" -> {
                vertx.undeploy(deploymentID());
            }
            case "user.add" -> {
                var id = json.getInteger("id");
                var infoJson = json.getJsonObject("userInfo");
                var user = json.containsKey("shadow") ? User.createShadow(id, star.universe(), star.index())
                        : User.getUserById(pool, id);
                var info = star.starInfo().starUsers.computeIfAbsent(id, i ->
                        infoJson == null ? new UserInfo() : infoJson.mapTo(UserInfo.class));
                info.online = true;
                info.x = 0;
                info.y = StarInfo.maxTier;
                assert user != null;
                commonContext.add(user);
                var state = commonContext.getState(id);
                if (json.containsKey("shadow")) {
                    state.isShadow = true;
                    state.input.relativePointer = new Position(0, 1);
                    info.spaceship.weapons.forEach(Weapon::unequip);
                    new Weapon(CargoStatic.r400.type()).equip(info.spaceship).tryEnable();
                }
                msg.reply(JsonObject.of("type", "success"));
            }
            case "user.disconnect" -> {
                var id = json.getInteger("id");
                var user = star.starInfo().starUsers.get(id);
                if (user == null) break;
                user.online = false;
                commonContext.remove(id);
                msg.reply(JsonObject.of("type", "success"));
            }
            case "user.remove" -> {
                var id = json.getInteger("id");
                var info = JsonObject.mapFrom(star.starInfo().starUsers.get(id));
                star.starInfo().starUsers.remove(id);
                commonContext.remove(id);
                msg.reply(JsonObject.of("type", "success", "userInfo", info));
            }
            case "user.update" -> {
                var id = json.getInteger("id");
                commonContext.getState(id).user = User.getUserById(pool, id);
            }
            case "user.message" -> {
                var userMsg = json.getJsonObject("msg");
                var type = userMsg.getString("type");
                var id = json.getInteger("userId");
                commonContext.getState(id).events.computeIfAbsent(type, i -> new ArrayList<>()).add(userMsg);
                commonContext.updated().users().add(id);
            }
        }
    }

    void mainLoop() {
        try {
            var startTime = System.nanoTime();
            deltaTime = startTime - prevTime;
            prevTime = startTime;
            commonContext.msgHandle.put(Math.max(0, msgHandleTime) / 1000_000.0);
            msgHandleTime = 0;
            if (!healthCheck()) return;
            mainBehavior.update(commonContext);
            mainBehavior.profilers.forEach((name, window) -> commonContext.profiles.put(name, window.getMean()));
            var seq = commonSeqRenderer.render(commonContext);
            drawableRenderer.profilers.forEach((name, window) -> commonContext.profiles.put(name, window.getMean()));
            var com = commonRenderer.render(commonContext);
            var spe = specialRenderer.render(commonContext);
            uiRenderer.profilers.forEach((name, window) -> commonContext.profiles.put(name, window.getMean()));
            eventBus.send(msgVertId, NoCopyBox.of(JsonObject.of(
                    "type", "star.updated",
                    "commonSeq", seq,
                    "common", com,
                    "special", spe)), new DeliveryOptions().setLocalOnly(true));
            commonContext.frame();
            updateTime = System.nanoTime() - startTime;
            commonContext.delta.put(deltaTime / 1000_000.0);
            commonContext.update.put(updateTime / 1000_000.0);
            if (updateTime > 1000_000_000 / MaxFps * 2) logger.warn(JsonObject.of(
                    "type", "update.largeFrame",
                    "updateTime", updateTime / 1000_000.0));
            var suspendTime = Math.max(1, ((long)(1000_000_000 / MaxFps) - updateTime) / 1000_000);
            commonContext.suspend.put(suspendTime);
            mainLoopId = vertx.setTimer(suspendTime, v -> mainLoop());
        } catch (Exception e) { // stop buggy logic
            logger.error(JsonObject.of(
                    "star.id", star.index(),
                    "msg", e.getLocalizedMessage()));
            e.printStackTrace();
            vertx.undeploy(deploymentID());
        }
    }

    private boolean healthCheck() { // health check (db connection & ownership)
        var summery = Star.getSummery(pool, star.index());
        assert summery != null;
        if (!Objects.equals(summery.vertId(), deploymentID())) {
            logger.error(JsonObject.of(
                    "type", "heathCheck.failed",
                    "star", star.name(),
                    "ownership", summery.vertId()));
            vertx.undeploy(deploymentID());
            return false;
        }
        return true;
    }

    void writeBack() {
        logger.info(JsonObject.of("type", "star.writeBack.start"));
        var start = System.nanoTime();
        tryWriteBack().onFailure(e -> {
            logger.info(JsonObject.of("type", "star.writeBack.failed",
                    "totalTime", (System.nanoTime() - start) / 1000_000.0));
            e.printStackTrace();
        }).onSuccess(r -> {
            if (!r) vertx.undeploy(deploymentID());
            else logger.info(JsonObject.of("type", "star.writeBack.end",
                    "totalTime", (System.nanoTime() - start) / 1000_000.0));
        });
        logger.info(JsonObject.of("type", "star.writeBack.forked",
                "blockTime", (System.nanoTime() - start) / 1000_000.0));
    }

    private Future<Boolean> tryWriteBack() {
        if (!commonContext.writeBackLock.compareAndExchange(false, true)) {
            try {
                var users = new ObjectMapper().writeValueAsString(star.starInfo().starUsers);
                return runBlocking(() -> writeBackSync(users), false);
            } catch (JsonProcessingException e) {
                return Future.failedFuture(e);
            } finally {
                commonContext.writeBackLock.set(false);
            }
        } else {
            return Future.failedFuture("write back locked");
        }
    }

    private Boolean writeBackSync(String users) throws IOException {
        var blocks = new String[StarUtils.blockNum];
        commonContext.areaStates.forEach(state -> {
            state.cached.forEach((id, cached) -> blocks[id] = cached);
        });
        var output = StarInfo.genStarInfo(users, blocks);
        return await(pool.preparedQuery(
                "update star set star_info = $1 where index = $2 and vert_id = $3;"
        ).execute(Tuple.of(DataStatic.gzipEncode(output), star.index(), context.deploymentID())))
                .rowCount() == 1;
    }

}
