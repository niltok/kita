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
    JsonObject prevMap = JsonObject.of("seq", 0);
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
            case "state.map.require" -> {
                eb.send(json.getString("socket"), JsonObject.of(
                        "type", "state.applyDiff",
                        "diff", JsonObject.of("/mapData", prevMap)
                ).toBuffer());
            }
        }
    }

    private void vertEventsHandler(Message<JsonObject> msg) {
        var json = msg.body();
        switch (json.getString("type")) {
            case "star.updated" -> {
                var star = json.getJsonObject("content");
                var map = JsonObject.of(
                        "seq", prevMap.getInteger("seq") + 1,
                        "starInfo", filterStarInfo(star.getJsonObject("starInfo")));
                var diff = jsonFlatDiff(JsonObject.of("mapData", prevMap), JsonObject.of("mapData", map));
                if (diff.size() == 1) break; // 只有 seq 变化，其他无变化，不用发送
                socket.forEach((k, v) -> {
                    eb.send(v, JsonObject.of(
                            "type", "state.applySeqDiff",
                            "seq", "/mapData/seq",
                            "diff", diff
                    ).toBuffer());
                });
                prevMap = map;
            }
        }
    }

    private JsonObject filterStarInfo(JsonObject starInfo) {
        return starInfo;
    }

    public static JsonObject jsonFlatDiff(JsonObject from, JsonObject to) {
        var merged = to.mergeIn(from);
        var diff = new HashMap<String, Object>();
        for (var kv : merged) {
        }
        return new JsonObject(diff);
    }
}
