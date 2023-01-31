package ikuyo.server.behaviors;

import ikuyo.server.api.Behavior;

public class AbstractBehavior implements Behavior {
    public Context context;

    @Override
    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    public void update() {}
}
