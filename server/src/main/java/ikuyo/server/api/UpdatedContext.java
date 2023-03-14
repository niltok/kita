package ikuyo.server.api;

import ikuyo.utils.Property;

import java.util.HashSet;
import java.util.Set;

public record UpdatedContext(Property<Boolean> init, Set<Integer> blocks, Set<Integer> users) {
    public UpdatedContext() {
        this(new Property<>(true), new HashSet<>(), new HashSet<>());
    }
    public void clear() {
        init.set(false);
        blocks().clear();
        users().clear();
    }
}
