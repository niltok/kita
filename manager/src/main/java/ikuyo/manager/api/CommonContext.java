package ikuyo.manager.api;

import io.reactivex.rxjava3.core.Observer;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.sqlclient.SqlClient;

import java.util.HashMap;
import java.util.Map;

public record CommonContext(
        Observer<Integer> render$,
        SqlClient sql,
        EventBus eventBus,
        UpdatedContext updated,
        Map<Integer, UserState> userState
) {
    public CommonContext(Observer<Integer> render$, SqlClient sql, EventBus eventBus, UpdatedContext updated) {
        this(render$, sql, eventBus, updated, new HashMap<>());
    }
}
