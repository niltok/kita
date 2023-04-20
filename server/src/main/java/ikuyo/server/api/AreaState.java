package ikuyo.server.api;

import java.util.HashMap;
import java.util.Map;

public class AreaState {
    public boolean loaded = false;
    public Map<Integer, String> cached = new HashMap<>();
}
