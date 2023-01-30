package ikuyo.server;

import ikuyo.utils.AsyncVerticle;
import ikuyo.utils.MsgDiffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MessageVert extends AsyncVerticle {
    EventBus eb;
    int starId;
    String updaterId;
    MessageConsumer<JsonObject> starEvents, vertEvents;
    MsgDiffer msgDiffer = new MsgDiffer("starDrawables");
    Map<Integer, Integer> specialCache = new HashMap<>();
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
                await(eb.request(updaterId, json));
                msg.reply(JsonObject.of("type", "user.add.success"));
            }
            case "user.disconnect" -> {
                socket.remove(json.getInteger("id"));
                await(eb.request(updaterId, json));
                if (socket.isEmpty())
                    eb.send(updaterId, JsonObject.of("type", "vert.undeploy"));
            }
            case "state.seq.require" -> eb.send(json.getString("socket"), msgDiffer.prev());
            case "user.message" -> userEventHandler(json);
        }
    }

    private void userEventHandler(JsonObject json) {
        var msg = json.getJsonObject("msg");
        switch (msg.getString("type")) {
        }
    }

    private void vertEventsHandler(Message<JsonObject> msg) {
        var json = msg.body();
        switch (json.getString("type")) {
            case "star.updated" -> {
                var drawables = json.getJsonObject("commonSeq").getJsonObject("starDrawables");
                var diff = msgDiffer.next(drawables);
                var specials = json.getJsonObject("special");
                socket.forEach((id, socket) -> {
                    if (diff != null) eb.send(socket, diff); // 只有 seq 变化、其他无变化则不用发送
                    var state = specials.getJsonObject(id.toString());
                    if (state != null) {
                        var hash = state.hashCode();
                        if (!Objects.equals(hash, specialCache.get(id))) { // 变化才发送
                            specialCache.put(id, hash);
                            eb.send(socket, JsonObject.of(
                                    "type", "state.dispatch",
                                    "action", "gameState/diffGame",
                                    "payload", JsonObject.of("star", state)
                            ).toBuffer());
                        }
                    }
                });
            }
        }
    }
}
