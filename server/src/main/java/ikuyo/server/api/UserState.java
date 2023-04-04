package ikuyo.server.api;

import ikuyo.api.User;
import ikuyo.api.UserInput;
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

    public void frame() {
        events = new HashMap<>();
        input.frame();
    }
}