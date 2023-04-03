package ikuyo.manager.behaviors;

import ikuyo.api.behaviors.Behavior;
import ikuyo.manager.api.BehaviorContext;

public class TransferBehavior implements Behavior<BehaviorContext> {
    @Override
    public void update(BehaviorContext context) {
        switch (context.event().getString("type")) {
            case "transfer.done" -> {
                context.context().updated().users().add(context.id());
                var state = context.context().userState().get(context.id());
                state.page = "";
            }
            case "auth.request", "user.move" -> {
                context.context().updated().users().add(context.id());
                var state = context.context().userState().get(context.id());
                state.page = "transfer";
            }
        }
    }
}
