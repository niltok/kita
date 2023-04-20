package ikuyo.server.api;

import java.util.HashMap;
import java.util.Map;

public class AreaState {
    public static final boolean workSet = false;
    public volatile boolean loaded = false, enabled = false;
    public Map<Integer, String> cached = new HashMap<>();
}
