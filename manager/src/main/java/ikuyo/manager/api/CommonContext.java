package ikuyo.manager.api;

import java.util.HashMap;
import java.util.Map;

public record CommonContext(UpdatedContext updated, Map<Integer, UserState> userState) {
    public CommonContext(UpdatedContext updated) {
        this(updated, new HashMap<>());
    }
}
