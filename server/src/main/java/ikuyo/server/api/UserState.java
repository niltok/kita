package ikuyo.server.api;

import ikuyo.api.datatypes.UserInput;
import ikuyo.api.entities.User;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class UserState {
    public Map<String, List<JsonObject>> events = new HashMap<>();
    public User user;
    public UserInput input = new UserInput();
    public String page = "";
    public boolean isShadow = false;

    public UserState(User user) {
        this.user = user;
    }

    public boolean frame() {
        var update = input.frame();
        events = new HashMap<>();
        var rand = new Random();
        if (isShadow) {
            if (rand.nextBoolean()) input.up = 1;
            if (rand.nextBoolean()) input.down = 1;
            if (rand.nextBoolean()) input.left = 1;
            if (rand.nextBoolean()) input.right = 1;
            if (rand.nextBoolean()) input.shot = 1;
            update = true;
        }
        return update;
    }
}
