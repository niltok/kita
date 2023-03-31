package ikuyo.server.api;

import ikuyo.api.Star;
import ikuyo.api.StarInfo;
import ikuyo.api.User;
import ikuyo.api.UserInput;
import io.vertx.core.Vertx;

import java.util.HashMap;
import java.util.Map;

public record CommonContext(
        Vertx vertx,
        Star star,
        Map<Integer, User> users,
        Map<Integer, UserInput> userInputs,
        UpdatedContext updated,
        PhysicsEngine engine
) {
    public CommonContext(Vertx vertx, Star star) {
        this(vertx, star, new HashMap<>(), new HashMap<>(), new UpdatedContext(), new PhysicsEngine());
    }
    public void remove(Integer id) {
        users().remove(id);
        engine().removeUser(id);
        userInputs().remove(id);
        updated().users().add(id);
    }
    public void add(Integer id, User user) {
        users().put(id, user);
        engine().addUser(user, star().starInfo().starUsers.get(id));
        userInputs().computeIfAbsent(id, i -> new UserInput());
        updated().users().add(id);
    }
    public void frame() {
        userInputs().forEach((i, u) -> u.frame());
        getInfos().forEach((id, info) -> {
            for (var weapon : info.spaceship.weapons) {
                weapon.frame();
            }
        });
        updated().clear();
    }

    public StarInfo.StarUserInfo getInfo(int id) {
        return star().starInfo().starUsers.get(id);
    }

    public Map<Integer, StarInfo.StarUserInfo> getInfos() {
        return star().starInfo().starUsers;
    }
}
