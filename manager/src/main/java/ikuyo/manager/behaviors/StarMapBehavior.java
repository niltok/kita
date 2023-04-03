package ikuyo.manager.behaviors;

import ikuyo.api.behaviors.Behavior;
import ikuyo.manager.api.BehaviorContext;

public class StarMapBehavior implements Behavior<BehaviorContext> {
    @Override
    public void update(BehaviorContext context) {
        switch (context.event().getString("type")) {
            case "starMap.toggle" -> {
                var state = context.getState(context.id());
                if ("transfer".equals(state.page)) break;
                context.updated().users().add(context.id());
                state.page = "starMap".equals(state.page) ? "" : "starMap";
            }
        }
    }
}
