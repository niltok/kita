package ikuyo.manager.api;

import ikuyo.api.User;
import io.vertx.core.json.JsonObject;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserState {
    public User user;
    public String socket, page = "transfer", nextPage = "";
    public int starFocus, pageEdge = 3;
    public JsonObject cache = JsonObject.of();
    public Map<String, List<JsonObject>> events = new HashMap<>();
    public UserState(String socket, User user) {
        this.socket = socket;
        this.user = user;
    }
    public boolean frame() {
        var update = false;
        events = new HashMap<>();
        if (pageEdge == 3) {
            pageEdge = 2;
            update = true;
        }
        if (pageEdge == 1) {
            page = nextPage;
            pageEdge = page.isEmpty() ? 0 : 3;
            update = true;
        }
        if ("techTrainer".equals(page) && Duration.ofMillis(user.techTree().availableTime()).toMinutes() < 10)
            update = true;
        return update;
    }
    public void setPage(String page) {
        if (this.page.isEmpty() && !page.isEmpty()) {
            pageEdge = 3;
            this.page = page;
        } else {
            pageEdge = 1;
            nextPage = page;
        }
    }
    public boolean allowOperate() {
        return !"transfer".equals(page);
    }
}
