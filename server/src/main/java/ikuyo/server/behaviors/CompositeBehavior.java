package ikuyo.server.behaviors;

import ikuyo.server.api.Behavior;

public class CompositeBehavior extends AbstractBehavior {
    Behavior[] behaviors;
    public CompositeBehavior(Behavior... behaviors) {
        this.behaviors = behaviors;
    }

    @Override
    public void setContext(Context context) {
        super.setContext(context);
        for (Behavior behavior : behaviors) {
            behavior.setContext(context);
        }
    }

    @Override
    public void update() {
        for (Behavior behavior : behaviors) {
            behavior.update();
        }
    }
}
