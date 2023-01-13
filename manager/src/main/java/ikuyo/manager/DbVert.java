package ikuyo.manager;

import ikuyo.api.Star;
import ikuyo.api.Universe;
import ikuyo.api.User;
import ikuyo.utils.AsyncVerticle;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import java.util.Objects;

public class DbVert extends AsyncVerticle {
    PgPool pool;

    //language=PostgreSQL
    static final String cleanDbSql = """
            drop schema public cascade;
            create schema public;
            grant all on schema public to postgres;
            grant all on schema public to public;
            """;

    @Override
    public void start() {
        pool = PgPool.pool(vertx, new PoolOptions());
        var count = await(pool
                .query("select * from pg_tables where schemaname = 'public';")
                .execute()).rowCount();
        if (!(count == 0 || Objects.equals(System.getenv("DB_MODE"), "reset"))) return;
        await(pool.query(String.join("",
                cleanDbSql,
                Universe.createTableSql,
                Star.StarGroup.createTableSql,
                Star.createTableSql,
                User.createTableSql
        )).execute());
    }
}
