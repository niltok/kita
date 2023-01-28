package ikuyo.server;

import com.google.common.hash.Hashing;
import ikuyo.api.Drawable;
import ikuyo.api.Star;
import ikuyo.server.utils.CompositeBehavior;
import ikuyo.server.utils.Behavior;
import ikuyo.utils.AsyncVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Tuple;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.vertx.await.Async.await;

public class UpdateVert extends AsyncVerticle {
    final double MaxFps = 60;
    EventBus eb;
    Star star;
    MessageConsumer<JsonObject> vertEvents;
    long writeBackId, mainLoopId, updateTime, prevTime, deltaTime, startTime, updateCount = 0;
    String msgVertId;
    PgPool pool;
    Behavior mainBehavior = new CompositeBehavior();

    @Override
    public void start() {
        eb = vertx.eventBus();
        pool = PgPool.pool(vertx, new PoolOptions());
        loadStar(config().getInteger("id"));
    }

    @Override
    public void stop() throws Exception {
        logger.info(JsonObject.of(
                "type", "updater.undeploy",
                "updateCount", updateCount,
                "averageTime", (System.nanoTime() - startTime) / 1000_000.0 / updateCount));
        vertx.cancelTimer(writeBackId);
        vertx.cancelTimer(mainLoopId);
        await(CompositeFuture.all(
                vertEvents.unregister(),
                vertx.undeploy(msgVertId),
                pool.preparedQuery("update star set vert_id = null where vert_id = $1;")
                        .execute(Tuple.of(deploymentID()))
                        .compose(rows -> pool.close())));
    }

    private void loadStar(int id) {
        star = Star.get(pool, id);
        await(pool.preparedQuery(
                "update star set vert_id = $2 where index = $1"
        ).execute(Tuple.of(id, deploymentID())));
        mainBehavior.start(new Behavior.Context(star));
        logger.info("star." + id + " loaded");
        vertEvents = eb.localConsumer(deploymentID(), this::vertEventsHandler);
        msgVertId = await(vertx.deployVerticle(MessageVert.class, new DeploymentOptions()
                .setWorker(true)
                .setConfig(JsonObject.of("updaterId", deploymentID(), "starId", id))));
        startTime = System.nanoTime();
        prevTime = startTime;
        mainLoopId = vertx.setTimer(1, v -> mainLoop());
        writeBackId = vertx.setPeriodic(5000, ignore -> writeBack());
    }

    private void vertEventsHandler(Message<JsonObject> msg) {
        var json = msg.body();
        switch (json.getString("type")) {
            case "undeploy" -> {
                vertx.undeploy(deploymentID());
            }
        }
    }

    void mainLoop() {
        try {
            var startTime = System.nanoTime();
            deltaTime = startTime - prevTime;
            prevTime = startTime;
            mainBehavior.update();
            eb.send(msgVertId, JsonObject.of(
                    "type", "star.updated",
                    "prevUpdateTime", updateTime,
                    "prevDeltaTime", deltaTime,
                    "drawables", JsonObject.mapFrom(render())));
            updateCount++;
            updateTime = System.nanoTime() - startTime;
            mainLoopId = vertx.setTimer(Math.max(1, ((long)(1000_000_000 / MaxFps) - updateTime) / 1000_000),
                    v -> mainLoop());
        } catch (Exception e) { // meet unloaded star or stop buggy logic
            if (star == null) return;
            logger.error(JsonObject.of(
                    "star.id", star.index(),
                    "msg", e.getLocalizedMessage()));
            e.printStackTrace();
        }
        if (logger.isTraceEnabled())
            logger.trace(JsonObject.of(
                "type", "update.end",
                "starId", star.index(),
                "updateTime", updateTime / 1000_000.0,
                "deltaTime", deltaTime / 1000_000.0));
    }

    JsonObject render() {
        var drawables = new ArrayList<Drawable>();
        for (var i = 0; i < star.starInfo().blocks.length; i++) {
            var block = star.starInfo().blocks[i];
            var d = new Drawable.Sprite();
            d.bundle = "blocks";
            d.asset = String.valueOf(block.id);
            drawables.add(d);
        }
        return new JsonObject(drawables.stream().map(JsonObject::mapFrom).collect(Collectors.toMap(
                json -> String.valueOf(json.hashCode()),
                Function.identity(),
                (s, a) -> s
        )));
    }

    void writeBack() {
        try {
            var buf = star.starInfo().toBuffer();
            var success = await(pool.preparedQuery(
                    "update star set star_info = $1 where index = $2 and vert_id = $3;"
            ).execute(Tuple.of(buf, star.index(), context.deploymentID()))).rowCount() == 1;
            if (!success) vertx.undeploy(deploymentID());
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());
            e.printStackTrace();
            vertx.undeploy(deploymentID());
        }
    }
}
