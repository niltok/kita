package ikuyo.server;

import ikuyo.utils.AsyncVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;

public class AppVert extends AsyncVerticle {
    EventBus eb;
    MessageConsumer<JsonObject> starNone;

    @Override
    public void startAsync() {
        eb = vertx.eventBus();
        starNone = eb.consumer("star.none", msg -> {
            var json = msg.body();
            switch (json.getString("type")) {
                case "star.load" -> {
                    var config = JsonObject.of("id", json.getInteger("id"));
                    await(vertx.deployVerticle(RenderVert.class, new DeploymentOptions()
                            .setWorker(true)
                            .setConfig(config)));
                    msg.reply(JsonObject.of("type", "star.load.success"));
                }
            }
        });
    }
}
