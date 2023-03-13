package ikuyo.api;

import com.google.common.hash.Hashing;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

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
public record Star(int index, String name, int universe, double x, double y, double z, StarInfo starInfo, int seed, String vertId) {
    public static final double cover = 40, viewRange = 20;
    static final int nameLength = 5;
    /**
     * 用于标注宇宙 auto expand 的边界的星团
     * @param universe
     * @param posX 覆盖范围 {@link Star#cover}，等效实际范围 (posX - 0.5) * cover ~ (posX + 0.5) * cover (ly)
     * @param posY 同 posX
     */
    public record StarGroup(int universe, int posX, int posY) {
        static final double starNumAvg = 10 * Math.pow(cover / viewRange / 2, 2), starNumDelta = starNumAvg / 5;
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
            // 乐观锁
            var succ = await(client.preparedQuery("insert into star_group(universe, pos_x, pos_y) values ($1, $2, $3)")
                    .execute(Tuple.of(universe, posX, posY))).rowCount() == 1;
            if (!succ) return;
            var rand = new Random(Hashing.goodFastHash(128).newHasher()
                    .putInt(universe).putInt(posX).putInt(posY).putInt(seed).hash().asLong());
            var fs = new LinkedList<Future>();
            var starCount = starNumAvg + starNumDelta * rand.nextGaussian();
            if (starCount < 1) {
                if (rand.nextDouble() >= starCount) return;
                Star.insert(client, universe,
                        (posX + rand.nextDouble() - 0.5) * cover,
                        (posY + rand.nextDouble() - 0.5) * cover,
                        2 * rand.nextDouble() - 1, rand.nextInt());
                return;
            }
            for (int i = 0; i < starCount; i++) {
                fs.add(async(() -> Star.insert(client, universe,
                        (posX + rand.nextDouble() - 0.5) * cover,
                        (posY + rand.nextDouble() - 0.5) * cover,
                        2 * rand.nextDouble() - 1, rand.nextInt())));
            }
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
            name text not null,
            universe int references universe not null,
            x double precision not null,
            y double precision not null,
            z double precision not null,
            star_info bytea default null,
            seed int not null,
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
                row.getInteger("index"), row.getString("name"), row.getInteger("universe"),
                row.getDouble("x"), row.getDouble("y"), row.getDouble("z"),
                StarInfo.fromJson(row.getBuffer("star_info")),
                row.getInteger("seed"), row.getString("vert_id"));
    }

    public static Star getSummery(SqlClient client, int index) {
        var rows = await(client.preparedQuery("""
            select index, name, universe, x, y, z, seed, vert_id
            from star
            where index = $1
            """).execute(Tuple.of(index)));
        if (rows.rowCount() == 0) return null;
        var row = rows.iterator().next();
        return new Star(
                row.getInteger("index"), row.getString("name"), row.getInteger("universe"),
                row.getDouble("x"), row.getDouble("y"), row.getDouble("z"),
                null, row.getInteger("seed"), row.getString("vert_id"));
    }

    public static void insert(SqlClient client, int universe, double x, double y, double z, int seed) {
//        Buffer starInfo = await(Vertx.currentContext().executeBlocking(p ->
//                p.complete(StarInfo.gen(seed).toBuffer()), false));
        var rand = new Random(seed);
        var sb = new StringBuilder(nameLength);
        for (int i = 0; i < nameLength; i++) {
            var v = rand.nextInt(0, 36);
            sb.append((char)(v < 10 ? '0' + v : 'A' + v - 10));
        }
        await(client.preparedQuery("""
            insert into star(name, universe, x, y, z, seed)
            values ($1, $2, $3, $4, $5, $6)
            """).execute(Tuple.of(sb.toString(), universe, x, y, z, seed)));
    }

    /** 不包含 starInfo, vertId 的 Star 位置信息，若所属 universe 启用了 autoExpand 则会自动生成未访问过的星星 */
    public static Star[] query(SqlClient client, int universe, double x1, double x2, double y1, double y2) {
        var uni = Universe.get(client, universe);
        assert uni != null;
        var fs = new ArrayList<Future>();
        if (uni.autoExpand()) {
            int x1g = (int) Math.floor(x1 / cover + 0.5),
                    x2g = (int) Math.ceil(x2 / cover + 0.5),
                    y1g = (int) Math.floor(y1 / cover + 0.5),
                    y2g = (int) Math.ceil(y2 / cover + 0.5);
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
                    select index, name, universe, x, y, z, seed
                    from star
                    where universe = $1 and (x between $2 and $3) and (y between $4 and $5);""")
                .execute(Tuple.of(universe, x1, x2, y1, y2)));
        var res = new Star[rows.rowCount()];
        var i = 0;
        for (Row row : rows) {
            res[i] = new Star(row.getInteger("index"), row.getString("name"), universe,
                    row.getDouble("x"), row.getDouble("y"), row.getDouble("z"),
                    null, row.getInteger("seed"), null);
            i++;
        }
        return res;
    }
}
