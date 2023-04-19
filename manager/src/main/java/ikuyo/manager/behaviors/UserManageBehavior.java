package ikuyo.manager.behaviors;

import ikuyo.api.behaviors.Behavior;
import ikuyo.manager.api.CommonContext;
import io.vertx.core.json.JsonObject;

import java.util.Random;

public class UserManageBehavior implements Behavior<CommonContext> {
    @Override
    public void update(CommonContext context) {
        var rand = new Random();
        context.updated().users().forEach(id -> {
            var state = context.getState(id);
            if (state == null) return;
            var addShadowMsgs = state.events.get("user.add.shadow");
            if (addShadowMsgs != null) addShadowMsgs.forEach(msg -> {
                var num = Integer.parseInt(msg.getJsonObject("states").getString("shadow-add-num"));
                for (var i = 0; i < num; i++) context.eventBus().send(state.starAddress(), JsonObject.of(
                        "type", "user.add",
                        "id", rand.nextInt(Integer.MIN_VALUE, 0),
                        "shadow", true));
            });
        });
    }
}
