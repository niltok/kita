package ikuyo.api;

import com.google.common.hash.Hashing;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;
import java.util.random.RandomGenerator;

import static ikuyo.utils.AsyncStatic.async;
import static io.vertx.await.Async.await;

/**
 * 星球基本信息
 * @param index
 * @param universe
 * @param x
 * @param y
 * @param z -1 ~ 1 (ly)
 * @param starInfo 以文本保存的详细信息（无需索引）
 */
public record Star(int index, int universe, double x, double y, double z, StarInfo starInfo, String vertId) {
    /**
     * 用于标注宇宙 auto expand 的边界的星团
     * @param universe
     * @param posX 覆盖范围 100ly 等效实际范围 posX * 100 - 50 ~ posX * 100 + 50 (ly)
     * @param posY 同 posX
     */
    public record StarGroup(int universe, int posX, int posY) {
        static final int starNumAvg = 2000, starNumDelta = 500;
        //language=PostgreSQL
        public static final String createTableSql = """
                create table star_group(
                    universe int references universe not null,
                    pos_x int not null,
                    pos_y int not null,
                    primary key (universe, pos_x, pos_y)
                );
                """;
        public static void insert(SqlClient client, int universe, int posX, int posY, int seed) {
            var rand = new Random(Hashing.goodFastHash(128).newHasher()
                    .putInt(universe).putInt(posX).putInt(posY).putInt(seed).hash().asLong());
            var fs = new LinkedList<Future>();
            for (int i = 0; i < starNumAvg + starNumDelta * rand.nextGaussian(); i++) {
                fs.add(async(() -> Star.insert(client, universe,
                        (posX + rand.nextDouble()) * 100 - 50,
                        (posY + rand.nextDouble()) * 100 - 50,
                        2 * rand.nextDouble() - 1, rand.nextInt())));
            }
            fs.add(client.preparedQuery("insert into star_group(universe, pos_x, pos_y) values ($1, $2, $3)")
                    .execute(Tuple.of(universe, posX, posY)));
            await(CompositeFuture.all(fs));
        }
        public static boolean exist(SqlClient client, int universe, int posX, int posY) {
            return await(client
                    .preparedQuery("select * from star_group where universe = $1 and pos_x = $2 and pos_y = $3;")
                    .execute(Tuple.of(universe, posX, posY))).rowCount() == 1;
        }
    }

    //language=PostgreSQL
    public static final String createTableSql = """
        create table star(
            index serial primary key,
            universe int references universe not null,
            x double precision not null,
            y double precision not null,
            z double precision not null,
            star_info bytea not null,
            vert_id text
        );
        """;

    public static Star get(SqlClient client, int index) {
        var rows = await(client.preparedQuery(
                "select * from star where index = $1"
        ).execute(Tuple.of(index)));
        if (rows.rowCount() == 0) return null;
        var row = rows.iterator().next();
        return new Star(
                row.getInteger("index"), row.getInteger("universe"),
                row.getDouble("x"), row.getDouble("y"), row.getDouble("z"),
                StarInfo.fromJson(row.getBuffer("star_info")),
                row.getString("vert_id"));
    }

    public static void insert(SqlClient client, int universe, double x, double y, double z, int seed) {
        await(client.preparedQuery("insert into star(universe, x, y, z, star_info) values ($1, $2, $3, $4, $5)")
                .execute(Tuple.of(universe, x, y, z, StarInfo.gen(seed).toBuffer())));
    }

    private static int posFromLy(double a) {
        return (int) (a + 50) / 100;
    }

    /** 不包含 starInfo, vertId 的 Star 位置信息，若所属 universe 启用了 autoExpand 则会自动生成未访问过的星星 */
    public static Star[] query(SqlClient client, int universe, double x1, double x2, double y1, double y2) {
        var uni = Universe.get(client, universe);
        assert uni != null;
        var fs = new ArrayList<Future>();
        if (uni.autoExpand()) {
            int x1g = posFromLy(x1), x2g = posFromLy(x2), y1g = posFromLy(y1), y2g = posFromLy(y2);
            for (int x = x1g; x <= x2g; x++) {
                for (int y = y1g; y <= y2g; y++) {
                    if (StarGroup.exist(client, universe, x, y)) continue;
                    int finalX = x, finalY = y;
                    fs.add(async(() -> StarGroup.insert(client, universe, finalX, finalY, uni.seed())));
                }
            }
            await(CompositeFuture.all(fs));
        }
        var rows = await(client.preparedQuery("""
                    select index, universe, x, y, z
                    from star
                    where universe = $1 and (x between $2 and $3) and (y between $4 and $5);""")
                .execute(Tuple.of(universe, x1, x2, y1, y2)));
        var res = new Star[rows.rowCount()];
        var i = 0;
        for (Row row : rows) {
            res[i] = new Star(row.getInteger("index"), universe, row.getDouble("x"),
                    row.getDouble("y"), row.getDouble("z"), null, null);
            i++;
        }
        return res;
    }
}
