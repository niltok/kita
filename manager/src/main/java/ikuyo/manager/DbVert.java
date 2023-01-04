package ikuyo.manager;

import ikuyo.api.Star;
import ikuyo.api.Universe;
import ikuyo.api.User;
import io.vertx.await.Async;
import io.vertx.core.AbstractVerticle;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;

import java.util.Objects;

import static io.vertx.await.Async.await;

public class DbVert extends AbstractVerticle {
    Async async;
    PgPool pool;

    @Override
    public void start() throws Exception {
        async = new Async(vertx);
        async.run(v -> startAsync());
    }

    //language=PostgreSQL
    static final String cleanDbSql = """
            drop schema public cascade;
            create schema public;
            grant all on schema public to postgres;
            grant all on schema public to public;
            """;

    void startAsync() {
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
