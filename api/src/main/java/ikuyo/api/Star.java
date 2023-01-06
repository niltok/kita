package ikuyo.api;

import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;

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
        //language=PostgreSQL
        public static final String createTableSql = """
                create table star_group(
                    universe int references universe not null,
                    pos_x int not null,
                    pos_y int not null,
                    primary key (universe, pos_x, pos_y)
                );
                """;
    }

    //language=PostgreSQL
    public static final String createTableSql = """
        create table star(
            index serial primary key,
            universe int references universe not null,
            x double precision not null,
            y double precision not null,
            z double precision not null,
            star_info text not null,
            vert_id text
        );
        insert into star(universe, x, y, z, star_info) values (1, 0, 0, 0, '%s');
        """.formatted(StarInfo.genStar(0).toString());

    public static Star getStar(SqlClient client, int index) {
        var row = await(client.preparedQuery(
                "select * from star where index = $1"
        ).execute(Tuple.of(index))).iterator().next();
        return new Star(
                row.getInteger("index"), row.getInteger("universe"),
                row.getDouble("x"), row.getDouble("y"), row.getDouble("z"),
                StarInfo.fromJson(row.getString("star_info")),
                row.getString("vert_id"));
    }
}
