package ikuyo.manager.api;

import ikuyo.api.User;
import io.vertx.core.json.JsonObject;

public class UserState {
    public User user;
    public String socket, page;
    public JsonObject cache = JsonObject.of();
    public UserState(String socket, User user) {
        this.socket = socket;
        this.user = user;
        page = "transfer";
    }
}
