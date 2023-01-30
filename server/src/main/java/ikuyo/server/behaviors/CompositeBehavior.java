package ikuyo.server.behaviors;

import ikuyo.server.api.Behavior;

public class CompositeBehavior extends AbstractBehavior {
    Behavior[] behaviors;
    public CompositeBehavior(Behavior... behaviors) {
        this.behaviors = behaviors;
    }

    @Override
    public void start(Context context) {
        super.start(context);
        for (Behavior behavior : behaviors) {
            behavior.start(context);
        }
    }

    @Override
    public void update() {
        for (Behavior behavior : behaviors) {
            behavior.update();
        }
    }
}
