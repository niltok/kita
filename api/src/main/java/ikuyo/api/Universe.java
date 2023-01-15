package ikuyo.api;

import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;

import static io.vertx.await.Async.await;

public record Universe(int index, int seed, boolean autoExpand) {
    //language=PostgreSQL
    public static final String createTableSql = """
        create table universe(
            index serial primary key,
            seed int not null default random(),
            auto_expand boolean not null
        );
        insert into universe(auto_expand) values (true);
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
}
