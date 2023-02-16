package ikuyo.server.api;

import ikuyo.utils.Property;

import java.util.ArrayList;
import java.util.List;

public record UpdatedContext(Property<Boolean> init, List<Integer> blocks, List<Integer> users) {
    public UpdatedContext() {
        this(new Property<>(true), new ArrayList<>(), new ArrayList<>());
    }
    public void clear() {
        init.set(false);
        blocks().clear();
        users().clear();
    }
}
