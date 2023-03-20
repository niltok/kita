package ikuyo.server;

import ikuyo.api.Position;
import ikuyo.utils.AsyncVerticle;
import ikuyo.utils.MsgDiffer;
import ikuyo.utils.NoCopyBox;
import io.vertx.core.CompositeFuture;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static ikuyo.utils.AsyncStatic.delay;

class UserState {
    JsonObject specialCache = JsonObject.of();
    Set<MsgDiffer.LongPair> drawableCache = new HashSet<>();
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
    MsgDiffer msgDiffer = new MsgDiffer();
    Map<Integer, UserState> userStates = new HashMap<>();
    JsonObject commonCache = JsonObject.of();

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
        if (enableMsgLog) logger.info(json);
        switch (json.getString("type")) {
            case "ping" -> msg.reply(JsonObject.of("type", "pong"));
            case "user.add" -> {
                userStates.put(json.getInteger("id"), new UserState(json.getString("socket")));
                await(eventBus.request(updaterId, json));
                msg.reply(JsonObject.of("type", "user.add.success"));
            }
            case "user.remove" -> {
                var id = json.getInteger("id");
                var userState = userStates.get(id);
                await(eventBus.request(updaterId, json));
                eventBus.send(userState.socket, JsonObject.of(
                        "type", "state.dispatch",
                        "action", "gameState/diffGame",
                        "payload", JsonObject.of("star",
                                MsgDiffer.jsonDiff(userState.specialCache, JsonObject.of()))
                ).toBuffer());
                eventBus.send(userState.socket, JsonObject.of(
                        "type", "seq.operate",
                        "data", msgDiffer.removeAll(userState.drawableCache)).toBuffer());
                userStates.remove(id);
                msg.reply(JsonObject.of("type", "success"));
                if (userStates.isEmpty()) {
                    await(delay(Duration.ofMinutes(5)));
                    if (userStates.isEmpty())
                        eventBus.send(updaterId, JsonObject.of("type", "vert.undeploy"));
                }
            }
            case "user.disconnect" -> {
                userStates.remove(json.getInteger("id"));
                await(eventBus.request(updaterId, json));
                if (userStates.isEmpty()) {
                    await(delay(Duration.ofMinutes(5)));
                    if (userStates.isEmpty())
                        eventBus.send(updaterId, JsonObject.of("type", "vert.undeploy"));
                }
            }
            case "user.update" -> {
                eventBus.send(updaterId, json);
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
                // TODO: concurrency frame barrier
                var drawables = json.getJsonObject("commonSeq");
                msgDiffer.next(drawables);
                var common = json.getJsonObject("common");
                var cdiff = MsgDiffer.jsonDiff(commonCache, common);
                commonCache = common;
                var specials = json.getJsonObject("special");
//                var fs = new ArrayList<Future>();
                userStates.forEach((id, userState) -> {
                    sendUserState(specials, cdiff, id, userState);
//                    fs.add(runBlocking(() -> sendUserState(specials, id, userState)));
                });
//                await(CompositeFuture.all(fs));
            }
        }
    }

    private void sendUserState(JsonObject specials, JsonObject cdiff, int id, UserState userState) {
        var msg = JsonArray.of();
        if (userState.specialCache.isEmpty()) msg.add(JsonObject.of(
                "type", "socket.echo",
                "payload", JsonObject.of("type", "transfer.done")));
        var state = specials.getJsonObject(String.valueOf(id));
        if (state != null) {
            var specialDiff = MsgDiffer.jsonDiff(userState.specialCache, state).mergeIn(cdiff, true);
            userState.specialCache = state;
            if (!specialDiff.isEmpty()) { // 变化才发送
                msg.add(JsonObject.of(
                        "type", "state.dispatch",
                        "action", "gameState/diffGame",
                        "payload", specialDiff
                ));
            }
        }
        var camera_ = userState.specialCache
                .getJsonObject("star")
                .getJsonObject("camera");
        if (camera_ == null) {
            return;
        }
        var cx = camera_.getInteger("x");
        var cy = camera_.getInteger("y");
        var moved = cx != userState.camera.x || cy != userState.camera.y;
        userState.camera.x = cx;
        userState.camera.y = cy;
        var diff = msgDiffer.query(id, userState.camera, moved, userState.drawableCache);
        if (diff != null) {
            msg.add(JsonObject.of(
                    "type", "seq.operate",
                    "data", diff
            ));
        }
        if (!msg.isEmpty()) {
            if (enableMsgLog) logger.info(msg);
            eventBus.send(userState.socket, msg.toBuffer());
        }
    }
}
