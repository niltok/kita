package ikuyo.api.behaviors;

import ikuyo.utils.AsyncHelper;

public interface Behavior<T> extends AsyncHelper {
    int windowSize = 60;
    void update(T context);
}
