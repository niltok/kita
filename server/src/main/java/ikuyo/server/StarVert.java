package ikuyo.server;

import ikuyo.api.Star;
import ikuyo.utils.AsyncVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

import static io.vertx.await.Async.await;

public class StarVert extends AsyncVerticle {
    EventBus eb;
    Star star;
    MessageConsumer<JsonObject> starMsgBox, starNone;
    String nodeId;
    long mainLoopId;
    // 发往这个地址的内容必须序列化为 Buffer 或 String
    Map<Integer, String> socket = new HashMap<>();

    @Override
    public void startAsync() {
        nodeId = config().getString("nodeId");
        eb = vertx.eventBus();
        starNone = eb.consumer("star.none");
        starNone.handler(this::starNoneHandler);
    }

    private void starNoneHandler(Message<JsonObject> msg) {
        var json = msg.body();
        switch (json.getString("type")) {
            case "star.load" -> {
                starNone.pause();
                int id = json.getInteger("id");
                // TODO: star = ; // load star
                System.out.println("star." + id + " loaded");
                starMsgBox = eb.consumer("star." + id);
                starMsgBox.handler(this::starMsgBoxHandler);
                mainLoopId = vertx.setPeriodic(1000 / 60, ignore -> mainLoop());
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
                vertx.cancelTimer(mainLoopId);
                await(starMsgBox.unregister());
                starNone.resume();
                star = null;
                starMsgBox = null;
                msg.reply(JsonObject.of("type", "star.unload.success"));
            }
        }
    }

    void mainLoop() {
        socket.forEach((k, v) -> {
            // eb.send(v, JsonObject.of("type", "ping").toBuffer());
        });
    }
}
