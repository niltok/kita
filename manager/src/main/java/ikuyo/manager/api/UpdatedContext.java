package ikuyo.manager.api;

import java.util.HashSet;
import java.util.Set;

public class UpdatedContext {
    public boolean init = true;
    public Set<Integer> users = new HashSet<>();
    public UpdatedContext() {}
    public Set<Integer> users() {
        return users;
    }
    public void clear() {
        init = false;
        users = new HashSet<>();
    }
}
