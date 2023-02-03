package ikuyo.manager;

import ikuyo.api.Star;
import ikuyo.api.Universe;
import ikuyo.api.User;
import ikuyo.utils.AsyncVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;

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
        resetDB();
    }

    private void resetDB() {
        var resetMode = System.getenv("DB_RESET");
        switch (resetMode) {
            case "empty" -> {
                var count = await(pool
                        .query("select * from pg_tables where schemaname = 'public';")
                        .execute()).rowCount();
                if (count != 0) return;
                // break & reset
            }
            case "always" -> {} // break & reset
            case null, default -> { return; } // never reset
        }
        logger.info(JsonObject.of("type", "reset database..."));
        await(pool.query(String.join("",
                cleanDbSql,
                Universe.createTableSql,
                Star.StarGroup.createTableSql,
                Star.createTableSql
        )).execute());
        logger.info(JsonObject.of("type", "creating initial star group..."));
        Star.query(pool, 1, 0, 0, 0, 0);
        await(pool.query(String.join("",
                User.createTableSql
        )).execute());
        logger.info(JsonObject.of("type", "database reset done"));
    }
}
