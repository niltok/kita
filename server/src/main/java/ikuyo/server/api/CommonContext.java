package ikuyo.server.api;

import ikuyo.api.Star;
import ikuyo.api.StarInfo;
import ikuyo.api.User;
import ikuyo.utils.StarUtils;
import io.vertx.core.Vertx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record CommonContext(
        Vertx vertx,
        Star star,
        Map<Integer, UserState> userStates,
        List<AreaState> areaStates,
        UpdatedContext updated,
        PhysicsEngine engine
) {
    public CommonContext(Vertx vertx, Star star) {
        this(vertx, star, new HashMap<>(), new ArrayList<>(StarUtils.areaNum), new UpdatedContext(), new PhysicsEngine());
        for (int i = 0; i < StarUtils.areaNum * 10; i++) areaStates.add(new AreaState());
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
        updated().clear();
        getInfos().forEach((id, info) -> {
            if (info.frame()) updated().users().add(id);
        });
        userStates().forEach((i, s) -> s.frame());
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
