package ikuyo.server.api;

import ikuyo.api.datatypes.UserInput;
import ikuyo.api.entities.User;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserState {
    public Map<String, List<JsonObject>> events = new HashMap<>();
    public User user;
    public UserInput input = new UserInput();
    public String page = "";

    public UserState(User user) {
        this.user = user;
    }

    public boolean frame() {
        var update = false;
        events = new HashMap<>();
        update |= input.frame();
        return update;
    }
}
