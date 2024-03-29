package ikuyo.manager;

import ikuyo.api.entities.Star;
import ikuyo.api.entities.StationCargo;
import ikuyo.api.entities.Universe;
import ikuyo.api.entities.User;
import ikuyo.utils.AsyncVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;


public class DbVert extends AsyncVerticle {
    final boolean SingleStar = false;
    PgPool pool;

    //language=PostgreSQL
    static final String cleanDbSql = """
            drop schema public cascade;
            create schema public;
            grant all on schema public to postgres;
            grant all on schema public to public;
            """;

    @Override
    public void start() throws Exception {
        pool = PgPool.pool(vertx, new PoolOptions());
        resetDB();
    }

    private void resetDB() throws Exception {
        Thread.sleep(3 * 1000);
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
                Star.createTableSql,
                User.createTableSql,
                StationCargo.createTableSql
        )).execute());
        logger.info(JsonObject.of("type", "creating universe..."));
        int univId = Universe.insert(pool, !SingleStar);
        if (SingleStar) Star.insert(pool, univId, 0, 0, 0, 0);
        logger.info(JsonObject.of("type", "creating users..."));
        User.insert(pool, "admin", "admin", true, univId, 1);
        User.insert(pool, "user0", "user0", false, univId, 1);
        logger.info(JsonObject.of("type", "database reset done"));
    }
}
