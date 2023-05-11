package ikuyo.server.api;

import ikuyo.api.datatypes.BaseContext;
import ikuyo.api.datatypes.UserInfo;
import ikuyo.api.entities.Star;
import ikuyo.api.entities.User;
import ikuyo.utils.StarUtils;
import ikuyo.utils.WindowSum;
import io.vertx.core.Vertx;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

import static ikuyo.api.behaviors.Behavior.windowSize;

public final class CommonContext extends BaseContext {
    public final Star star;
    private final Map<Integer, UserState> userStates = new HashMap<>();
    public final List<AreaState> areaStates = new ArrayList<>();
    public final WindowSum delta = new WindowSum(windowSize),
            update = new WindowSum(windowSize),
            message = new WindowSum(windowSize),
            frameTime = new WindowSum(windowSize);
    public final Map<String, Double> profiles = new HashMap<>();
    private final UpdatedContext updated = new UpdatedContext();
    private final PhysicsEngine dynamicEngine = new PhysicsEngine();
    private final PhysicsEngine staticEngine = new PhysicsEngine();
    public final Set<Integer> admin = new HashSet<>();
    public volatile Set<Integer> enabledAreas = new ConcurrentSkipListSet<>();
    public int areaDelta;
    public AtomicBoolean writeBackLock = new AtomicBoolean(false);

    public CommonContext(Vertx vertx, Star star) {
        super(vertx);
        this.star = star;
        for (int i = 0; i < StarUtils.areaNum; i++) areaStates.add(new AreaState());
    }

    public void remove(Integer id) {
        dynamicEngine().removeUser(id);
        userStates.remove(id);
        admin.remove(id);
        updated().users().add(id);
    }

    public void add(User user) {
        int id = user.id();
        dynamicEngine().addUser(user, star().starInfo().starUsers.get(id));
        userStates.computeIfAbsent(id, i -> new UserState(user));
        if (user.isAdmin()) admin.add(id);
        updated().users().add(id);
    }

    public void frame() {
        var startTime = System.nanoTime();
        updated().clear();
        updated().users().addAll(admin);
        getInfos().forEach((id, info) -> {
            if (info.frame()) updated().users().add(id);
        });
        userStates.forEach((id, state) -> {
            if (state.frame()) updated().users().add(id);
            if (state.isShadow) {
                var weapon = getInfo(id).spaceship.getCurrentWeapon();
                weapon.ammoAmount = weapon.getAmmoMax();
            }
        });
        frameTime.put((System.nanoTime() - startTime) / 1000_000.);
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

    public PhysicsEngine dynamicEngine() {
        return dynamicEngine;
    }

    public PhysicsEngine staticEngine() {
        return staticEngine;
    }
}
