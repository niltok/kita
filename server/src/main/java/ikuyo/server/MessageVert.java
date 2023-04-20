package ikuyo.server;

import ikuyo.utils.AsyncVerticle;
import ikuyo.utils.MsgDiffer;
import ikuyo.utils.NoCopyBox;
import ikuyo.utils.Position;
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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static ikuyo.utils.AsyncStatic.delay;

public class MessageVert extends AsyncVerticle {
    public static final boolean reserveStar = false;
    public static final Duration reserveTime = Duration.ofMinutes(5);
    int starId;
    String updaterId;
    MessageConsumer<JsonObject> starEvents;
    MessageConsumer<NoCopyBox<JsonObject>> vertEvents;
    MsgDiffer msgDiffer = new MsgDiffer();
    Map<Integer, UserState> userStates = new HashMap<>();
    JsonObject commonCache = JsonObject.of(), drawableCache = JsonObject.of();
    Lock barrier = new ReentrantLock(true);

    static class UserState {
        JsonObject specialCache = JsonObject.of();
        Set<MsgDiffer.LongPair> drawableCache = new HashSet<>();
        Position camera = new Position();
        /** 发往这个地址的内容必须序列化为 Buffer 或 String */
        String socket;
        public UserState(String socket) {
            this.socket = socket;
        }
    }

    @Override
    public void start() throws Exception {
        starId = config().getInteger("starId");
        updaterId = config().getString("updaterId");
        starEvents = eventBus.consumer("star." + starId, this::starEventsHandler);
        vertEvents = eventBus.localConsumer(deploymentID(), msg -> {
            try {
                vertEventsHandler(msg);
            } catch (Throwable e) {
                logger.error(e.getLocalizedMessage());
                e.printStackTrace();
            }
        });
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
                var res = await(eventBus.request(updaterId, json)).body();
                if (userState.socket != null) eventBus.send(userState.socket, JsonObject.of(
                        "type", "state.dispatch",
                        "action", "gameState/diffGame",
                        "payload", JsonObject.of("star",
                                MsgDiffer.jsonDiff(userState.specialCache, JsonObject.of()))
                ).toBuffer());
                lock(barrier);
                try {
                    if (userState.socket != null) eventBus.send(userState.socket, JsonObject.of(
                            "type", "seq.operate",
                            "data", msgDiffer.removeAll(userState.drawableCache)).toBuffer());
                } finally {
                    barrier.unlock();
                }
                userStates.remove(id);
                msg.reply(res);
                if (userStates.isEmpty()) {
                    if (reserveStar) await(delay(reserveTime));
                    if (userStates.isEmpty())
                        eventBus.send(updaterId, JsonObject.of("type", "vert.undeploy"));
                }
            }
            case "user.disconnect" -> {
                userStates.remove(json.getInteger("id"));
                await(eventBus.request(updaterId, json));
                if (userStates.isEmpty()) {
                    if (reserveStar) await(delay(reserveTime));
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
                lock(barrier);
                var startTime = System.nanoTime();
                try {
                    var drawables = json.getJsonObject("commonSeq");
                    if (enableMsgLog) {
                        var ddiff = MsgDiffer.jsonDiff(drawableCache, drawables);
                        if (!ddiff.isEmpty()) logger.info(drawables);
                        drawableCache = drawables;
                    }
                    msgDiffer.next(drawables);
                    var common = json.getJsonObject("common");
                    var specials = json.getJsonObject("special");
//                    parallelFor(userStates.entrySet().stream(), e ->
//                        runBlocking(() -> sendUserState(
//                                specials.getJsonObject(String.valueOf(e.getKey())),
//                                common, e.getKey(), e.getValue()), false)
//                    );
                    userStates.entrySet().stream().parallel().forEach(e ->
                            runBlocking(() -> sendUserState(
                                    specials.getJsonObject(String.valueOf(e.getKey())),
                                    common, e.getKey(), e.getValue()), false)
                    );
                } finally {
                    barrier.unlock();
                    var sendTime = System.nanoTime() - startTime;
                    eventBus.send(updaterId, JsonObject.of("type", "message.frame", "time", sendTime));
                    if (sendTime > 1000_000_000 / UpdateVert.MaxFps * 2)
                        logger.info(JsonObject.of(
                                "type", "message.largeFrame",
                                "sendTime", sendTime / 1000_000.));
                }
            }
        }
    }

    private void sendUserState(JsonObject special, JsonObject common, int id, UserState userState) {
        var msg = JsonArray.of();
        if (userState.specialCache.isEmpty()) msg.add(JsonObject.of(
                "type", "socket.echo",
                "payload", JsonObject.of("type", "transfer.done")));
        if (special == null) {
            special = JsonObject.of();
        }
        if (common != null) special.mergeIn(common, true);
        if (!special.isEmpty()) { // 变化才发送
            msg.add(JsonObject.of(
                    "type", "state.dispatch",
                    "action", "gameState/diffGame",
                    "payload", special
            ));
        }
        userState.specialCache.mergeIn(special, true);
        var star_ = userState.specialCache.getJsonObject("star");
        var camera_ = star_ == null ? null : star_.getJsonObject("camera");
        if (camera_ == null) {
            camera_ = JsonObject.of(
                    "x", userState.camera.x,
                    "y", userState.camera.y);
        }
        var pos = new Position(camera_.getInteger("x"), camera_.getInteger("y"));
        var diff = msgDiffer.query(id, userState.camera, pos, userState.drawableCache);
        userState.camera = pos;
        if (diff != null) {
            msg.add(JsonObject.of(
                    "type", "seq.operate",
                    "data", diff
            ));
        }
        if (!msg.isEmpty()) {
//            if (enableMsgLog) logger.info(msg);
            if (userState.socket != null) eventBus.send(userState.socket, msg.toBuffer());
        }
    }
}
