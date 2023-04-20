package ikuyo.manager.api;

import ikuyo.api.datatypes.BaseContext;
import ikuyo.api.entities.Star;
import ikuyo.api.entities.User;
import ikuyo.utils.AsyncHelper;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static ikuyo.utils.AsyncStatic.delay;

public final class CommonContext extends BaseContext implements AsyncHelper {
    public void addUser(String socket, User user) {
        socketCache().put(socket, user);
        var state = userState().computeIfAbsent(user.id(), i -> new UserState(socket, user));
        state.user = user;
        updated().users().add(user.id());
    }

    public User getUser(String socket) {
        return socketCache().get(socket);
    }

    public UserState getState(int id) {
        return userState().get(id);
    }

    public void frame() {
        updated().clear();
        userState().forEach((id, state) -> {
            if (state.frame()) updated().users().add(id);
        });
    }

    static final int timeout = 10000;
    private final SqlClient sql;
    private final EventBus eventBus;
    private final Logger logger;
    private final UpdatedContext updated;
    private final Map<Integer, UserState> userState;
    private final Map<String, User> socketCache;

    public CommonContext(
            Vertx vertx,
            SqlClient sql,
            EventBus eventBus,
            Logger logger
    ) {
        super(vertx);
        this.sql = sql;
        this.eventBus = eventBus;
        this.logger = logger;
        this.updated = new UpdatedContext();
        this.userState = new HashMap<>();
        this.socketCache = new HashMap<>();
    }

    public void registerUser(User user, String socket, JsonObject info, int retry) {
        try {
            var summery = Star.getSummery(sql(), user.star());
            assert summery != null;
            async(() -> Star.query(sql(), user.universe(),
                    summery.x() - Star.viewRange, summery.x() + Star.viewRange,
                    summery.y() - Star.viewRange, summery.y() + Star.viewRange));
            if (summery.vertId() == null) {
                await(eventBus.request("star.none", JsonObject.of(
                        "type", "star.load", "id", user.star()
                ), new DeliveryOptions().setSendTimeout(timeout)));
            }
            await(eventBus.request("star." + user.star(), JsonObject.of(
                    "type", "user.add", "socket", socket, "id", user.id(), "userInfo", info
            ), new DeliveryOptions().setSendTimeout(timeout)));
        } catch (Exception e) {
            // 尝试切换节点再加载
            if (retry < 3) {
                // time lock
                var res = await(sql().preparedQuery("""
                        update star set vert_id = null, time_lock = now() + interval '15 seconds'
                        where index = $1 and time_lock < now()
                        """).execute(Tuple.of(user.star()))).rowCount();
                if (res > 0) logger.info(JsonObject.of(
                        "type", "star.ownership.release", "starId", user.star()));
            }
            if (retry > 0) {
                await(delay(Duration.ofSeconds(3)));
                logger.info(JsonObject.of("type", "user.register.retry", "remain", retry));
                registerUser(user, socket, info, retry - 1);
            } else throw new RuntimeException("server busy");
        }
    }

    public SqlClient sql() {
        return sql;
    }

    public EventBus eventBus() {
        return eventBus;
    }

    public Logger logger() {
        return logger;
    }

    public UpdatedContext updated() {
        return updated;
    }

    public Map<Integer, UserState> userState() {
        return userState;
    }

    public Map<String, User> socketCache() {
        return socketCache;
    }
}
