package ikuyo.server;

import ikuyo.utils.AsyncVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class MessageVert extends AsyncVerticle {
    EventBus eb;
    int starId;
    String updaterId;
    MessageConsumer<JsonObject> starEvents, vertEvents;
    /** 发往这个地址的内容必须序列化为 Buffer 或 String */
    Map<Integer, String> socket = new HashMap<>();

    @Override
    public void start() throws Exception {
        eb = vertx.eventBus();
        starId = config().getInteger("starId");
        updaterId = config().getString("updaterId");
        starEvents = eb.consumer("star." + starId, this::starEventsHandler);
        vertEvents = eb.localConsumer(deploymentID(), this::vertEventsHandler);
    }

    private void starEventsHandler(Message<JsonObject> msg) {
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
                if (socket.isEmpty())
                    eb.send(updaterId, JsonObject.of("type", "undeploy"));
            }
            case "user.operate.map" -> {}
        }
    }

    private void vertEventsHandler(Message<JsonObject> msg) {
        var json = msg.body();
        switch (json.getString("type")) {
            case "star.updated" -> {
                socket.forEach((k, v) -> {
                    eb.send(v, JsonObject.of(
                            "type", "state.applyDiff",
                            "diff", JsonObject.of("/mapData", json.getJsonObject("content"))
                    ).toBuffer());
                });
            }
        }
    }
}
