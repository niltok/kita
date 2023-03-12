package ikuyo.manager.api;

import io.vertx.core.Vertx;
import io.vertx.sqlclient.SqlClient;

import java.util.HashMap;
import java.util.Map;

public record CommonContext(SqlClient sql, UpdatedContext updated, Map<Integer, UserState> userState) {
    public CommonContext(SqlClient sql, UpdatedContext updated) {
        this(sql, updated, new HashMap<>());
    }
}
