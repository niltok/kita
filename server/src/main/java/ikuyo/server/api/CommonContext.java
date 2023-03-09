package ikuyo.server.api;

import ikuyo.api.Star;
import ikuyo.api.User;

import java.util.Map;

public record CommonContext(
        Star star,
        Map<Integer, User> users,
        UpdatedContext updated
) {
}
