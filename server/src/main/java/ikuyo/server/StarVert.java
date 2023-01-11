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
import java.util.UUID;

import static io.vertx.await.Async.await;

public class StarVert extends AsyncVerticle {
    final double MaxFps = 60;
    EventBus eb;
    Star star;
    MessageConsumer<JsonObject> starMsgBox, starNone;
    String nodeId, vertId;
    long writeBackId, frameTime;
    PgPool pool;
    // 发往这个地址的内容必须序列化为 Buffer 或 String
    Map<Integer, String> socket = new HashMap<>();
    Renderer mainRenderer = new CompositeRenderer();

    @Override
    public void startAsync() {
        nodeId = config().getString("nodeId");
        vertId = UUID.randomUUID().toString();
        eb = vertx.eventBus();
        pool = PgPool.pool(vertx, new PoolOptions());
        starNone = eb.consumer("star.none");
        starNone.handler(this::starNoneHandler);
    }

    private void starNoneHandler(Message<JsonObject> msg) {
        var json = msg.body();
        switch (json.getString("type")) {
            case "star.load" -> {
                starNone.pause();
                int id = json.getInteger("id");
                star = Star.getStar(pool, id);
                await(pool.preparedQuery(
                        "update star set vert_id = $2 where index = $1"
                ).execute(Tuple.of(id, vertId)));
                mainRenderer.init(new Renderer.Context(star));
                System.out.println("star." + id + " loaded");
                starMsgBox = eb.consumer("star." + id);
                starMsgBox.handler(this::starMsgBoxHandler);
                vertx.runOnContext(v -> mainLoop());
                writeBackId = vertx.setPeriodic(5000, ignore -> writeBack());
                msg.reply(JsonObject.of("type", "star.load.success"));
            }
            case "close" -> {
                starNone.pause();
            }
        }
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
            case "star.unload" -> {
                unload();
                msg.reply(JsonObject.of("type", "star.unload.success"));
            }
        }
    }

    private void unload() {
        vertx.cancelTimer(writeBackId);
        await(starMsgBox.unregister());
        starNone.resume();
        star = null;
        starMsgBox = null;
    }

    void mainLoop() {
        try {
            var startTime = System.nanoTime();
            mainRenderer.render();
            socket.forEach((k, v) -> {
                // eb.send(v, JsonObject.of("type", "ping").toBuffer());
            });
            frameTime = System.nanoTime() - startTime;
            vertx.setTimer(Math.max(0, (long)(1000000 / MaxFps) - frameTime) / 1000,
                    v -> mainLoop());
        } catch (Exception e) { // meet unloaded star or stop buggy logic
            if (star == null) return;
            logger.error(JsonObject.of(
                    "star.id", star.index(),
                    "msg", e.getLocalizedMessage()));
            e.printStackTrace();
        }
        System.gc();
    }

    void writeBack() {
        var str = JsonObject.mapFrom(star.starInfo()).toString();
        try {
            var success = await(pool.preparedQuery(
                    "update star set star_info = $1 where index = $2 and vert_id = $3"
            ).execute(Tuple.of(str, star.index(), vertId))).rowCount() == 1;
            if (!success) unload();
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());
            e.printStackTrace();
            unload();
        }
    }
}
