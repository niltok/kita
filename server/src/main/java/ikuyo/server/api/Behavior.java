package ikuyo.server.api;

import ikuyo.api.Star;
import ikuyo.api.UserKeyInput;
import ikuyo.utils.AsyncHelper;

import java.util.Map;

public interface Behavior extends AsyncHelper {
    record Context(Star star, Map<Integer, UserKeyInput> userKeyInputs) {}
    void setContext(Context context);
    void update();
}
