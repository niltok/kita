package ikuyo.manager.api;

import ikuyo.api.User;
import io.vertx.core.json.JsonObject;

public class UserState {
    public User user;
    public String socket;
    public JsonObject cache = JsonObject.of();
    public boolean starMapDisplay = false, techTrainerDisplay = false;
    public UserState(String socket, User user) {
        this.socket = socket;
        this.user = user;
    }
}
