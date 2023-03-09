package ikuyo.manager.behaviors;

import ikuyo.api.behaviors.Behavior;
import ikuyo.manager.api.BehaviorArgContext;

public class StarMapBehavior implements Behavior<BehaviorArgContext> {
    @Override
    public void update(BehaviorArgContext context) {
        switch (context.event().getString("type")) {
            case "starMap.toggle" -> {
                context.context().common().updated().users().add(context.id());
                var state = context.context().common().userState().get(context.id());
                state.mapDisplay = !state.mapDisplay;
            }
        }
    }
}
