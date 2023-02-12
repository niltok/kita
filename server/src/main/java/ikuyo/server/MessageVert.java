package ikuyo.server;

import ikuyo.api.UserKeyInput;
import ikuyo.utils.AsyncVerticle;
import ikuyo.utils.MsgDiffer;
import io.vertx.core.CompositeFuture;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

class UserState {
    JsonObject specialCache = JsonObject.of();
    UserKeyInput keyInput = new UserKeyInput();
    /** 发往这个地址的内容必须序列化为 Buffer 或 String */
    String socket;
    public UserState(String socket) {
        this.socket = socket;
    }
}

public class MessageVert extends AsyncVerticle {
    int starId;
    String updaterId;
    MessageConsumer<JsonObject> starEvents, vertEvents;
    MsgDiffer msgDiffer = new MsgDiffer("starDrawables");
    Map<Integer, UserState> userStates = new HashMap<>();

    @Override
    public void start() throws Exception {
        starId = config().getInteger("starId");
        updaterId = config().getString("updaterId");
        starEvents = eventBus.consumer("star." + starId, this::starEventsHandler);
        vertEvents = eventBus.localConsumer(deploymentID(), this::vertEventsHandler);
    }

    @Override
    public void stop() throws Exception {
        await(CompositeFuture.all(
                starEvents.unregister(),
                vertEvents.unregister()));
    }

    private void starEventsHandler(Message<JsonObject> msg) {
        var json = msg.body();
//        logger.info(json);
        switch (json.getString("type")) {
            case "ping" -> msg.reply(JsonObject.of("type", "pong"));
            case "user.add" -> {
                userStates.put(json.getInteger("id"), new UserState(json.getString("socket")));
                eventBus.send(json.getString("socket"), msgDiffer.prev());
                await(eventBus.request(updaterId, json));
                msg.reply(JsonObject.of("type", "user.add.success"));
            }
            case "user.disconnect" -> {
                userStates.remove(json.getInteger("id"));
                await(eventBus.request(updaterId, json));
                if (userStates.isEmpty())
                    eventBus.send(updaterId, JsonObject.of("type", "vert.undeploy"));
            }
            case "user.message" -> userEventHandler(json);
        }
    }

    private void userEventHandler(JsonObject json) {
        var msg = json.getJsonObject("msg");
        switch (msg.getString("type")) {
            case "star.operate.key" -> {
                userStates.get(json.getInteger("userId")).keyInput.input(
                        msg.getString("action"), msg.getInteger("value", 1));
            }
        }
    }

    private void vertEventsHandler(Message<JsonObject> msg) {
        var json = msg.body();
        switch (json.getString("type")) {
            case "star.updated" -> {
                var drawables = json.getJsonObject("commonSeq").getJsonObject("starDrawables");
                var diff = msgDiffer.next(drawables);
                var specials = json.getJsonObject("special");
                userStates.forEach((id, userState) -> {
                    if (diff != null) { // 只有 seq 变化、其他无变化则不用发送
                        logger.info(diff);
                        eventBus.send(userState.socket, diff);
                    }
                    var state = specials.getJsonObject(id.toString());
                    if (state != null) {
                        var specialDiff = MsgDiffer.jsonDiff(userState.specialCache, state);
                        if (!specialDiff.isEmpty()) { // 变化才发送
                            userState.specialCache = state;
                            eventBus.send(userState.socket, JsonObject.of(
                                    "type", "state.dispatch",
                                    "action", "gameState/diffGame",
                                    "payload", JsonObject.of("star", specialDiff)
                            ).toBuffer());
                        }
                    }
                });
            }
            case "user.input.key.require" -> {
                msg.reply(new JsonObject(userStates.entrySet().stream().collect(Collectors.toMap(
                        e -> e.getKey().toString(),
                        e -> JsonObject.mapFrom(e.getValue().keyInput)))));
                userStates.forEach((id, u) -> u.keyInput.frame());
            }
        }
    }
}
