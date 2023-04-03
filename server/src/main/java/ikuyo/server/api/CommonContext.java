package ikuyo.server.api;

import ikuyo.api.Star;
import ikuyo.api.StarInfo;
import ikuyo.api.User;
import io.vertx.core.Vertx;

import java.util.HashMap;
import java.util.Map;

public record CommonContext(
        Vertx vertx,
        Star star,
        Map<Integer, UserState> userStates,
        UpdatedContext updated,
        PhysicsEngine engine
) {
    public CommonContext(Vertx vertx, Star star) {
        this(vertx, star, new HashMap<>(), new UpdatedContext(), new PhysicsEngine());
    }
    public void remove(Integer id) {
        engine().removeUser(id);
        userStates().remove(id);
        updated().users().add(id);
    }
    public void add(Integer id, User user) {
        engine().addUser(user, star().starInfo().starUsers.get(id));
        userStates().computeIfAbsent(id, i -> new UserState(user));
        updated().users().add(id);
    }
    public void frame() {
        getInfos().forEach((id, info) -> {
            for (var weapon : info.spaceship.weapons) {
                weapon.frame();
            }
        });
        userStates().forEach((i, s) -> s.frame());
        updated().clear();
    }

    public StarInfo.StarUserInfo getInfo(int id) {
        return getInfos().get(id);
    }

    public Map<Integer, StarInfo.StarUserInfo> getInfos() {
        return star().starInfo().starUsers;
    }

    public UserState getState(int id) {
        return userStates.get(id);
    }
}
