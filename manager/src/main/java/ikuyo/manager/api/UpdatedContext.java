package ikuyo.manager.api;

import ikuyo.utils.Property;

import java.util.ArrayList;
import java.util.List;

public record UpdatedContext(Property<Boolean> init, List<Integer> users) {
    public UpdatedContext() {
        this(new Property<>(false), new ArrayList<>());
    }
    public void clear() {
        init.set(false);
        users.clear();
    }
}
