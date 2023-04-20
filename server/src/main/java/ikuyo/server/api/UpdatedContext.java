package ikuyo.server.api;

import java.util.HashSet;
import java.util.Set;

public class UpdatedContext {
    public boolean init = true;
    public Set<Integer> blocks = new HashSet<>(), users = new HashSet<>(), areas = new HashSet<>();

    public UpdatedContext() {}

    public Set<Integer> blocks() {
        return blocks;
    }

    public Set<Integer> users() {
        return users;
    }

    public boolean init() {
        return init;
    }

    public void clear() {
        init = false;
        blocks = new HashSet<>();
        users = new HashSet<>();
    }
}
