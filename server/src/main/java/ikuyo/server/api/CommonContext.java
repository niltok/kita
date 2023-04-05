package ikuyo.server.api;

import ikuyo.api.datatypes.UserInfo;
import ikuyo.api.entities.Star;
import ikuyo.api.entities.User;
import ikuyo.utils.StarUtils;
import ikuyo.utils.WindowSum;
import io.vertx.core.Vertx;

import java.util.*;

public final class CommonContext {
    static int windowSize = 60;
    public final Vertx vertx;
    public final Star star;
    private final Map<Integer, UserState> userStates = new HashMap<>();
    public final List<AreaState> areaStates = new ArrayList<>();
    public final WindowSum delta = new WindowSum(windowSize);
    public final WindowSum update = new WindowSum(windowSize);
    private final UpdatedContext updated = new UpdatedContext();
    private final PhysicsEngine engine = new PhysicsEngine();
    public final Set<Integer> admin = new HashSet<>();

    public CommonContext(Vertx vertx, Star star) {
        this.vertx = vertx;
        this.star = star;
        for (int i = 0; i < StarUtils.areaNum; i++) areaStates.add(new AreaState());
    }

    public void remove(Integer id) {
        engine().removeUser(id);
        userStates.remove(id);
        admin.remove(id);
        updated().users().add(id);
    }

    public void add(User user) {
        int id = user.id();
        engine().addUser(user, star().starInfo().starUsers.get(id));
        userStates.computeIfAbsent(id, i -> new UserState(user));
        if (user.isAdmin()) admin.add(id);
        updated().users().add(id);
    }

    public void frame() {
        updated().clear();
        updated().users().addAll(admin);
        getInfos().forEach((id, info) -> {
            if (info.frame()) updated().users().add(id);
        });
        userStates.forEach((id, state) -> {
            if (state.frame()) updated().users().add(id);
        });
    }

    public UserInfo getInfo(int id) {
        return getInfos().get(id);
    }

    public Map<Integer, UserInfo> getInfos() {
        return star().starInfo().starUsers;
    }

    public UserState getState(int id) {
        return userStates.get(id);
    }

    public Star star() {
        return star;
    }

    public UpdatedContext updated() {
        return updated;
    }

    public PhysicsEngine engine() {
        return engine;
    }
}
