package ikuyo.server.utils;

import ikuyo.api.Star;
import ikuyo.utils.AsyncHelper;

public interface Renderer extends AsyncHelper {
    record Context(Star star) {}
    void init(Context context);
    void render();
}
