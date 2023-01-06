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
        insert into universe(auto_expand) values (false);
        """;

    public static boolean isAutoExpand(SqlClient client, int index) {
        return await(client.preparedQuery(
                "select * from universe where index = $1 and auto_expand = true"
        ).execute(Tuple.of(index))).rowCount() == 1;
    }
}
