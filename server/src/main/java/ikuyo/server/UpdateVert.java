package ikuyo.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ikuyo.api.Star;
import ikuyo.api.StarInfo;
import ikuyo.api.User;
import ikuyo.api.UserKeyInput;
import ikuyo.api.renderers.CompositeRenderer;
import ikuyo.api.renderers.Renderer;
import ikuyo.api.behaviors.CompositeBehavior;
import ikuyo.api.behaviors.Behavior;
import ikuyo.server.api.BehaviorContext;
import ikuyo.server.api.CommonContext;
import ikuyo.server.api.RendererContext;
import ikuyo.server.api.UpdatedContext;
import ikuyo.server.behaviors.ControlMovingBehavior;
import ikuyo.server.renderers.*;
import ikuyo.utils.AsyncVerticle;
import ikuyo.utils.DataStatic;
import ikuyo.utils.NoCopyBox;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Tuple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

public class UpdateVert extends AsyncVerticle {
    final double MaxFps = 80;
    Star star;
    MessageConsumer<JsonObject> vertEvents;
    long writeBackId, mainLoopId, updateTime, prevTime, deltaTime, startTime, updateCount = 0, updateTotalTime = 0;
    String msgVertId;
    PgPool pool;
    Behavior<BehaviorContext> mainBehavior = new CompositeBehavior<>(
            new ControlMovingBehavior()
    );
    BehaviorContext behaviorContext;
    Renderer<RendererContext> commonSeqRenderer = new ParallelRenderer(false,
            new ParallelRenderer(false,
                    new BlockRenderer(),
                    new UserRenderer()
            ).withName("starDrawables")
    );
    Renderer<RendererContext> specialRenderer = new ParallelRenderer(true,
            new CameraRenderer()
    );
    RendererContext rendererContext;
    UpdatedContext updatedContext;
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
                vertx.undeploy(msgVertId),
                async(this::stopPool)));
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
        star = Star.get(pool, id);
        updatedContext = new UpdatedContext();
        commonContext = new CommonContext(star, new HashMap<>(), updatedContext);
        behaviorContext = new BehaviorContext(new HashMap<>(), commonContext);
        rendererContext = new RendererContext(vertx, commonContext);
        await(pool.preparedQuery(
                "update star set vert_id = $2 where index = $1"
        ).execute(Tuple.of(id, deploymentID())));
        logger.info(JsonObject.of("type", "star.loaded", "id", id, "name", star.name()));
        vertEvents = eventBus.localConsumer(deploymentID(), this::vertEventsHandler);
        msgVertId = await(vertx.deployVerticle(MessageVert.class, new DeploymentOptions()
                .setConfig(JsonObject.of("updaterId", deploymentID(), "starId", id))));
        startTime = System.nanoTime();
        prevTime = startTime;
        mainLoopId = vertx.setTimer(1, v -> mainLoop());
        writeBackId = vertx.setPeriodic(20 * 60 * 1000, ignore -> writeBack());
    }

    private void vertEventsHandler(Message<JsonObject> msg) {
        var json = msg.body();
        logger.info(json);
        switch (json.getString("type")) {
            case "vert.undeploy" -> {
                vertx.undeploy(deploymentID());
            }
            case "user.add" -> {
                var id = json.getInteger("id");
                var user = User.getUserById(pool, id);
                commonContext.users().put(id, user);
                star.starInfo().starUsers.computeIfAbsent(id, i -> new StarInfo.StarUserInfo()).online = true;
                behaviorContext.userKeyInputs().computeIfAbsent(id, i -> new UserKeyInput());
                updatedContext.users().add(id);
                msg.reply(JsonObject.of("type", "success"));
            }
            case "user.disconnect" -> {
                var id = json.getInteger("id");
                var users = star.starInfo().starUsers;
                users.get(id).online = false;
                commonContext.users().remove(id);
                behaviorContext.userKeyInputs().remove(id);
                updatedContext.users().add(id);
                msg.reply(JsonObject.of("type", "success"));
            }
            case "user.remove" -> {
                var id = json.getInteger("id");
                star.starInfo().starUsers.remove(id);
                commonContext.users().remove(id);
                behaviorContext.userKeyInputs().remove(id);
                updatedContext.users().add(id);
                msg.reply(JsonObject.of("type", "success"));
            }
            case "user.message" -> userEventHandler(json);
        }
    }

    void userEventHandler(JsonObject json) {
        var msg = json.getJsonObject("msg");
        switch (msg.getString("type")) {
            case "star.operate.key" -> {
                var id = json.getInteger("userId");
                behaviorContext.userKeyInputs().get(id).input(
                        msg.getString("action"), msg.getInteger("value", 1));
            }
        }
    }

    void mainLoop() {
        try {
            var startTime = System.nanoTime();
            deltaTime = startTime - prevTime;
            prevTime = startTime;
            mainBehavior.update(behaviorContext);
            behaviorContext.userKeyInputs().forEach((i, u) -> u.frame());
            var seq = commonSeqRenderer.render(rendererContext);
            var spe = specialRenderer.render(rendererContext);
            eventBus.send(msgVertId, NoCopyBox.of(JsonObject.of(
                    "type", "star.updated",
                    "prevUpdateTime", updateTime,
                    "prevDeltaTime", deltaTime,
                    "commonSeq", seq,
                    "special", spe)));
            updatedContext.clear();
            updateCount++;
            updateTime = System.nanoTime() - startTime;
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
        if (logger.isTraceEnabled())
            logger.trace(JsonObject.of(
                "type", "update.end",
                "starId", star.index(),
                "updateTime", updateTime / 1000_000.0,
                "deltaTime", deltaTime / 1000_000.0));
    }

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
            ).execute(Tuple.of(DataStatic.gzipEncode(buf), star.index(), context.deploymentID()))).rowCount() == 1);
        } catch (JsonProcessingException e) {
            return Future.failedFuture(e);
        }
    }
}
