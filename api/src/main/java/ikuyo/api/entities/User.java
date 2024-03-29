package ikuyo.api.entities;

import ikuyo.api.datatypes.UserInfo;
import ikuyo.api.techtree.TechTree;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;

import static io.vertx.await.Async.await;

public record User(
        int id,
        String name,
        String pwd,
        boolean isAdmin,
        String token,
        int universe,
        int star,
        int station,
        TechTree techTree
) {
    //language=PostgreSQL
    static final String getByIdSql = """
            select * from "user" where id = $1;
            """;

    static public User createShadow(int id, int uni, int star) {
        return new User(id, "Shadow", "", true, "", uni, star, -1, new TechTree());
    }
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
                row.getInteger("universe"), row.getInteger("star"),
                row.getInteger("station"),
                TechTree.fromJson(row.getString("tech_tree")));
    }

    //language=PostgreSQL
    public static final String createTableSql = """
            create table "user"(
                id serial primary key,
                name text not null unique,
                pwd text not null,
                is_admin bool not null default false,
                token text not null default '',
                universe int references universe not null default 1,
                star int references star not null default 1,
                station int not null default 0,
                "info" bytea default null,
                tech_tree text not null
            );
            """;

    public static int insert(SqlClient client, String name, String pwd, boolean isAdmin, int univ, int star) {
        var id = await(client.preparedQuery("""
            insert into "user"(name, pwd, is_admin, universe, star, "info", tech_tree)
            values ($1, $2, $3, $4, $5, $6, $7) returning id;
            """).execute(Tuple.of(name, pwd, isAdmin, univ, star,
                    new UserInfo().toBuffer(), new TechTree().toString())))
                .iterator().next().getInteger(0);
        return id;
    }

    public static UserInfo getInfo(SqlClient client, int id) {
        return UserInfo.fromJson(await(client.preparedQuery("""
            select "info" from "user" where id = $1
            """).execute(Tuple.of(id))).iterator().next().getBuffer(0));
    }

    public static void putInfo(SqlClient client, int id, UserInfo info) {
        var buffer = info == null ? null : info.toBuffer();
        await(client.preparedQuery("""
            update "user" set "info" = $2 where id = $1
            """).execute(Tuple.of(id, buffer)));
    }
}
