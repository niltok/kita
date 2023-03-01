package ikuyo.server;

import ikuyo.api.Position;
import ikuyo.api.UserKeyInput;
import ikuyo.utils.AsyncVerticle;
import ikuyo.utils.MsgDiffer;
import ikuyo.utils.NoCopyBox;
import io.vertx.core.CompositeFuture;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class UserState {
    JsonObject specialCache = JsonObject.of();
    Set<String> drawableCache = new HashSet<>();
    Position camera = new Position();
    /** 发往这个地址的内容必须序列化为 Buffer 或 String */
    String socket;
    public UserState(String socket) {
        this.socket = socket;
    }
}

public class MessageVert extends AsyncVerticle {
    int starId;
    String updaterId;
    MessageConsumer<JsonObject> starEvents;
    MessageConsumer<NoCopyBox<JsonObject>> vertEvents;
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
        logger.info(json);
        switch (json.getString("type")) {
            case "ping" -> msg.reply(JsonObject.of("type", "pong"));
            case "user.add" -> {
                userStates.put(json.getInteger("id"), new UserState(json.getString("socket")));
//                eventBus.send(json.getString("socket"), msgDiffer.prev());
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
            default -> eventBus.send(updaterId, json);
        }
    }

    private void vertEventsHandler(Message<NoCopyBox<JsonObject>> msg) {
        var json = msg.body().value;
        switch (json.getString("type")) {
            case "star.updated" -> {
                var drawables = json.getJsonObject("commonSeq").getJsonObject("starDrawables");
                msgDiffer.next(drawables);
                var specials = json.getJsonObject("special");
                userStates.forEach((id, userState) -> {
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
                    var camera_ = userState.specialCache.getJsonObject("camera");
                    if (camera_ == null) {
                        return;
                    }
                    var cx = camera_.getInteger("x");
                    var cy = camera_.getInteger("y");
                    var moved = cx != userState.camera.x || cy != userState.camera.y;
                    userState.camera.x = cx;
                    userState.camera.y = cy;
                    var diff = msgDiffer.query(userState.camera, moved, userState.drawableCache);
                    if (diff != null) {
//                        logger.info(diff);
                        eventBus.send(userState.socket, diff);
                    }
                });
            }
        }
    }
}
