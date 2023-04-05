package ikuyo.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ikuyo.api.behaviors.Behavior;
import ikuyo.api.behaviors.CompositeBehavior;
import ikuyo.api.datatypes.StarInfo;
import ikuyo.api.datatypes.UserInfo;
import ikuyo.api.entities.Star;
import ikuyo.api.entities.User;
import ikuyo.api.renderers.CompositeRenderer;
import ikuyo.api.renderers.Renderer;
import ikuyo.api.renderers.UIRenderer;
import ikuyo.server.api.CommonContext;
import ikuyo.server.behaviors.*;
import ikuyo.server.renderers.*;
import ikuyo.utils.AsyncVerticle;
import ikuyo.utils.DataStatic;
import ikuyo.utils.NoCopyBox;
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

import java.util.ArrayList;
import java.util.Objects;

public class UpdateVert extends AsyncVerticle {
    public static final double MaxFps = 80;
    Star star;
    boolean loaded = false;
    MessageConsumer<JsonObject> vertEvents;
    long writeBackId, mainLoopId, updateTime, prevTime, deltaTime, startTime, updateCount = 0, updateTotalTime = 0;
    String msgVertId;
    PgPool pool;
    Behavior<CommonContext> mainBehavior = new CompositeBehavior<>(
            new KeyInputBehavior(),
            new ControlMovingBehavior(),
            new PhysicsEngineBehavior(),
            new PointerMovingBehavior(),
            new WeaponBehavior(),
            new UserAttackBehavior(),
            new BulletBehavior(),
            new PageBehavior()
    );
    Renderer<CommonContext> commonSeqRenderer = new CompositeRenderer<>(false,
            new DrawablesRenderer.Composite(
                    new BlockRenderer(),
                    new UserRenderer(),
                    new BulletRenderer()
            )
    );
    Renderer<CommonContext> commonRenderer = new CompositeRenderer<>(true);
    Renderer<CommonContext> specialRenderer = new CompositeRenderer<>(true,
            new CameraRenderer().withName("camera"),
            new UIRenderer.Composite<>(
                    new UserStateRenderer(),
                    new CargoRenderer()
            ).withName("ui")
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
                "starName", star.name(),
                "updateCount", updateCount,
                "averageDeltaTime", (System.nanoTime() - startTime) / 1000_000.0 / updateCount,
                "averageUpdateTime", updateTotalTime / 1000_000.0 / updateCount));
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
            try {
                vertEventsHandler(v);
            } catch (Exception e) {
                logger.warn(v.body());
                logger.error(e.getLocalizedMessage());
                e.printStackTrace();
            }
        });
        msgVertId = await(vertx.deployVerticle(MessageVert.class, new DeploymentOptions()
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
        writeBackId = vertx.setPeriodic(20 * 60 * 1000, ignore -> writeBack());
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
                var user = User.getUserById(pool, id);
                var info = star.starInfo().starUsers.computeIfAbsent(id, i ->
                        infoJson == null ? new UserInfo() : infoJson.mapTo(UserInfo.class));
                info.online = true;
                info.x = 0;
                info.y = StarInfo.minTier + 10;
                assert user != null;
                commonContext.add(user);
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
            if (!healthCheck()) return;
            mainBehavior.update(commonContext);
            var seq = commonSeqRenderer.render(commonContext);
            var com = commonRenderer.render(commonContext);
            var spe = specialRenderer.render(commonContext);
            eventBus.send(msgVertId, NoCopyBox.of(JsonObject.of(
                    "type", "star.updated",
                    "commonSeq", seq,
                    "common", com,
                    "special", spe)), new DeliveryOptions().setLocalOnly(true));
            commonContext.frame();
            updateCount++;
            updateTime = System.nanoTime() - startTime;
            commonContext.delta.put(deltaTime / 1000_000.0);
            commonContext.update.put(updateTime / 1000_000.0);
            if (updateTime > 1000_000_000 / MaxFps * 2) logger.warn(JsonObject.of(
                    "type", "update.largeFrame",
                    "updateTime", updateTime / 1000_000.0));
            updateTotalTime += updateTime;
            mainLoopId = vertx.setTimer(Math.max(1, ((long)(1000_000_000 / MaxFps) - updateTime) / 1000_000),
                    v -> mainLoop());
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

    // TODO: fix write back blocking
    void writeBack() {
        logger.info(JsonObject.of("type", "star.writeBack.start"));
        var start = System.nanoTime();
        tryWriteBack().onFailure(e -> {
            logger.error(e.getLocalizedMessage());
            e.printStackTrace();
            vertx.undeploy(deploymentID());
        }).onSuccess(r -> {
            if (!r) vertx.undeploy(deploymentID());
            else logger.info(JsonObject.of("type", "star.writeBack.writeEnd",
                    "totalTime", (System.nanoTime() - start) / 1000_000.0));
        });
        logger.info(JsonObject.of("type", "star.writeBack.blockEnd",
                "blockTime", (System.nanoTime() - start) / 1000_000.0));
    }

    private Future<Boolean> tryWriteBack() {
        try {
            var buf = new ObjectMapper().writeValueAsBytes(star.starInfo());
            return runBlocking(() -> await(pool.preparedQuery(
                    "update star set star_info = $1 where index = $2 and vert_id = $3;"
            ).execute(Tuple.of(DataStatic.gzipEncode(buf),
                    star.index(),
                    context.deploymentID()))).rowCount() == 1, false);
        } catch (JsonProcessingException e) {
            return Future.failedFuture(e);
        }
    }
}
