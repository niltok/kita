package ikuyo.manager.behaviors;

import ikuyo.api.behaviors.Behavior;
import ikuyo.manager.api.CommonContext;

public class TransferBehavior implements Behavior<CommonContext> {
    @Override
    public void update(CommonContext context) {
        context.updated().users().forEach(id -> {
            var state = context.getState(id);
            if (state == null) return;
            var transferredMsgs = state.events.get("transfer.done");
            if (transferredMsgs != null && !transferredMsgs.isEmpty()) {
                state.setPage("");
            }
        });
    }
}
