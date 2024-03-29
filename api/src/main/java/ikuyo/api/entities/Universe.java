package ikuyo.api.entities;

import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;

import static io.vertx.await.Async.await;

public record Universe(int index, int seed, boolean autoExpand) {
    //language=PostgreSQL
    public static final String createTableSql = """
        create table universe(
            index serial primary key,
            seed int not null default (random() - 0.5) * 4294967296,
            auto_expand boolean not null
        );
        """;

    public static Universe get(SqlClient client, int index) {
        var rows = await(client.preparedQuery(
                "select * from universe where index = $1"
        ).execute(Tuple.of(index)));
        if (rows.rowCount() == 0) return null;
        var row = rows.iterator().next();
        return new Universe(
                row.getInteger("index"),
                row.getInteger("seed"),
                row.getBoolean("auto_expand"));
    }

    public static int insert(SqlClient client, boolean autoExpand) {
        var id = await(client.preparedQuery(
                "insert into universe(auto_expand) values ($1) returning index;"
        ).execute(Tuple.of(autoExpand))).iterator().next().getInteger(0);
        if (!autoExpand) return id;
        Star.query(client, id, -5 * Star.cover, 5 * Star.cover, -5 * Star.cover, 5 * Star.cover);
        return id;
    }
}
