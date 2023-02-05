package ikuyo.api.behaviors;

import ikuyo.utils.AsyncHelper;

public interface Behavior<T> extends AsyncHelper {
    void update(T context);
}
