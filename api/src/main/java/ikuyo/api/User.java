package ikuyo.api;

import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;

import static io.vertx.await.Async.await;

public record User(int id, String name, String pwd, boolean isAdmin, String token, int universe, int star) {
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

    public static User getByName(SqlClient client, String name) {
        try {
            var rows = await(client.preparedQuery(getByNameSql).execute(Tuple.of(name)));
            return getUser(rows);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return null;
        }
    }

    //language=PostgreSQL
    static final String getByTokenSql = """
            select * from "user" where token = $1;
            """;

    public static User getByToken(SqlClient client, String token) {
        try {
            var rows = await(client.preparedQuery(getByTokenSql).execute(Tuple.of(token)));
            return getUser(rows);
        } catch (Exception e) {
            return null;
        }
    }

    private static User getUser(RowSet<Row> rows) {
        if (rows.rowCount() == 0) return null;
        var row = rows.iterator().next();
        return new User(row.getInteger("id"), row.getString("name"),
                row.getString("pwd"), row.getBoolean("is_admin"),
                row.getString("token"),
                row.getInteger("universe"), row.getInteger("star"));
    }

    //language=PostgreSQL
    public static final String createTableSql = """
            create table "user"(
                id serial primary key,
                name text not null unique,
                pwd text not null,
                is_admin bool not null,
                token text not null default '',
                universe int references universe not null default 1,
                star int references star not null default 1
            );
            insert into "user"(name, pwd, is_admin) values ('admin', 'admin', true);
            """;
}
