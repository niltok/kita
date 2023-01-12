package ikuyo.server;

import ikuyo.api.Star;
import ikuyo.server.utils.CompositeRenderer;
import ikuyo.server.utils.Renderer;
import ikuyo.utils.AsyncVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Tuple;

import java.util.HashMap;
import java.util.Map;

import static io.vertx.await.Async.await;

public class RenderVert extends AsyncVerticle {
    final double MaxFps = 60;
    EventBus eb;
    Star star;
    MessageConsumer<JsonObject> starMsgBox;
    long writeBackId, mainLoopId, frameTime, prevTime, deltaTime;
    PgPool pool;
    Renderer mainRenderer = new CompositeRenderer();
    /** 发往这个地址的内容必须序列化为 Buffer 或 String */
    Map<Integer, String> socket = new HashMap<>();

    @Override
    public void startAsync() {
        eb = vertx.eventBus();
        pool = PgPool.pool(vertx, new PoolOptions());
        loadStar(config().getInteger("id"));
    }

    @Override
    public void stopAsync() throws Exception {
        vertx.cancelTimer(writeBackId);
        vertx.cancelTimer(mainLoopId);
        await(starMsgBox.unregister());
    }

    private void loadStar(int id) {
        star = Star.getStar(pool, id);
        await(pool.preparedQuery(
                "update star set vert_id = $2 where index = $1"
        ).execute(Tuple.of(id, parentContext.deploymentID())));
        mainRenderer.init(new Renderer.Context(star));
        logger.info("star." + id + " loaded");
        starMsgBox = eb.consumer("star." + id);
        starMsgBox.handler(this::starMsgBoxHandler);
        prevTime = System.nanoTime();
        mainLoopId = vertx.setTimer(0, v -> mainLoop());
        writeBackId = vertx.setPeriodic(5000, ignore -> writeBack());
    }

    private void starMsgBoxHandler(Message<JsonObject> msg) {
        var json = msg.body();
        logger.info(json);
        switch (json.getString("type")) {
            case "ping" -> msg.reply(JsonObject.of("type", "pong"));
            case "user.add" -> {
                socket.put(json.getInteger("id"), json.getString("socket"));
                msg.reply(JsonObject.of("type", "user.add.success"));
            }
            case "user.disconnect" -> {
                socket.remove(json.getInteger("id"));
            }
        }
    }

    void mainLoop() {
        try {
            var startTime = System.nanoTime();
            deltaTime = startTime - prevTime;
            prevTime = startTime;
            mainRenderer.render();
            socket.forEach((k, v) -> {
                // eb.send(v, JsonObject.of("type", "ping").toBuffer());
            });
            System.gc();
            frameTime = System.nanoTime() - startTime;
            mainLoopId = vertx.setTimer(Math.max(0, (long)(1000000 / MaxFps) - frameTime) / 1000,
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
                "type", "frame.render.end",
                "starId", star.index(),
                "frameTime", frameTime,
                "deltaTime", deltaTime));
    }

    void writeBack() {
        var str = JsonObject.mapFrom(star.starInfo()).toString();
        try {
            var success = await(pool.preparedQuery(
                    "update star set star_info = $1 where index = $2 and vert_id = $3"
            ).execute(Tuple.of(str, star.index(), parentContext.deploymentID()))).rowCount() == 1;
            if (!success) vertx.undeploy(parentContext.deploymentID());
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());
            e.printStackTrace();
            vertx.undeploy(parentContext.deploymentID());
        }
    }
}
