package ikuyo.server;

import ikuyo.api.Star;
import ikuyo.utils.AsyncVerticle;
import io.vertx.await.Async;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;

import static io.vertx.await.Async.await;

public class StarVert extends AsyncVerticle {
    EventBus eb;
    Star star;
    MessageConsumer<JsonObject> starMsgBox, starNone;
    String nodeId, endpoint;

    @Override
    public void startAsync() {
        nodeId = config().getString("nodeId");
        endpoint = System.getenv("ENDPOINT");
        eb = vertx.eventBus();
        starNone = eb.consumer("star.none");
        starNone.handler(this::starNoneHandler);
        vertx.setPeriodic(1000 / 60, id -> mainLoop());
    }

    private void starNoneHandler(Message<JsonObject> msg) {
        var json = msg.body();
        switch (json.getString("type")) {
            case "load" -> {
                starNone.pause();
                int id = json.getInteger("id");
                // TODO: star = ;
                starMsgBox = eb.consumer("star." + id);
                starMsgBox.handler(this::starMsgBoxHandler);
                msg.reply(JsonObject.of("endpoint", endpoint));
            }
            case "close" -> {
                starNone.pause();
            }
        }
    }

    private void starMsgBoxHandler(Message<JsonObject> msg) {
        var json = msg.body();
        switch (json.getString("type")) {
            case "ping" -> {
                msg.reply(JsonObject.of("type", "pong", "endpoint", endpoint));
            }
            case "unload" -> {
                await(starMsgBox.unregister());
                starNone.resume();
                star = null;
                starMsgBox = null;
            }
        }
    }

    void mainLoop() {
        if (star == null) return;
    }
}
