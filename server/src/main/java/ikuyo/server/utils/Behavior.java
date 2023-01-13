package ikuyo.server.utils;

import ikuyo.api.Star;
import ikuyo.utils.AsyncHelper;

public interface Behavior extends AsyncHelper {
    record Context(Star star) {}
    void start(Context context);
    void update();
}
