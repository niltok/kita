package ikuyo.server.behaviors;

import ikuyo.api.behaviors.Behavior;
import ikuyo.server.api.CommonContext;

public class KeyInputBehavior implements Behavior<CommonContext> {
    @Override
    public void update(CommonContext context) {
        context.updated().users().forEach(id -> {
            var state = context.getState(id);
            if (state == null) return;
            var msgs = state.events.get("star.operate.key");
            if (msgs == null || msgs.isEmpty()) return;
            msgs.forEach(msg -> {
                state.input.input(msg.getString("action"), msg.getInteger("value", 1));
            });
        });
    }
}
