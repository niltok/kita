package ikuyo.manager.behaviors;

import ikuyo.api.behaviors.Behavior;
import ikuyo.manager.api.BehaviorContext;

public class StarMapBehavior implements Behavior<BehaviorContext> {
    @Override
    public void update(BehaviorContext context) {
        switch (context.event().getString("type")) {
        }
    }
}
