package ikuyo.server;

import ikuyo.utils.AsyncVerticle;
import ikuyo.utils.NoCopyBox;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;

public class AppVert extends AsyncVerticle {
    MessageConsumer<JsonObject> starNone;

    @Override
    public void start() {
        eventBus.registerDefaultCodec(NoCopyBox.class, new NoCopyBox.Codec());
        starNone = eventBus.consumer("star.none", msg -> {
            var json = msg.body();
            switch (json.getString("type")) {
                case "star.load" -> {
                    var config = JsonObject.of("id", json.getInteger("id"));
                    await(vertx.deployVerticle(UpdateVert.class, new DeploymentOptions()
                            .setWorker(true)
                            .setConfig(config)));
                    msg.reply(JsonObject.of("type", "star.load.success"));
                }
            }
        });
    }
}
