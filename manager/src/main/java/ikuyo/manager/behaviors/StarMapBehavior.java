package ikuyo.manager.behaviors;

import ikuyo.api.behaviors.Behavior;
import ikuyo.manager.api.BehaviorContext;

public class StarMapBehavior implements Behavior<BehaviorContext> {
    @Override
    public void update(BehaviorContext context) {
        switch (context.event().getString("type")) {
            case "starMap.toggle" -> {
                context.context().updated().users().add(context.id());
                var state = context.context().userState().get(context.id());
                state.starMapDisplay = !state.starMapDisplay;
            }
        }
    }
}
