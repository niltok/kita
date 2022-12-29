package ikuyo.api;

import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;
import org.jetbrains.annotations.NotNull;

import static io.vertx.await.Async.await;

public record User(int id, String name, String pwd, boolean isAdmin, int universe, int star) {
    //language=PostgreSQL
    static final String getByIdSql = """
            select * from "user" where id = $1;
            """;
    static public User getUserById(SqlClient client, int id) {
        try {
            var rows = await(client.preparedQuery(getByIdSql).execute(Tuple.of(id)));
            return getUser(rows);
        } catch (Exception e) {
            return null;
        }
    }

    //language=PostgreSQL
    static final String getByNameSql = """
            select * from "user" where name = $1;
            """;

    public static User getUserByName(SqlClient client, String name) {
        try {
            var rows = await(client.preparedQuery(getByNameSql).execute(Tuple.of(name)));
            return getUser(rows);
        } catch (Exception e) {
            return null;
        }
    }

    private static @NotNull User getUser(RowSet<Row> rows) {
        var row = rows.iterator().next();
        return new User(row.getInteger("id"), row.getString("name"),
                row.getString("pwd"), row.getBoolean("is_admin"),
                row.getInteger("universe"), row.getInteger("star"));
    }

    //language=PostgreSQL
    public static final String createTableSql = """
            create table "user"(
                id serial primary key,
                name text not null unique,
                pwd text not null,
                is_admin bool not null,
                universe int references universe default null,
                star int references star default null
            );
            insert into "user"(name, pwd, is_admin) values ('admin', 'admin', true);
            """;
}
