package ikuyo.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;

import java.util.List;
import java.util.Map;

import static io.vertx.await.Async.await;

/**
 * 货舱
 * @param belongs 货舱拥有者
 * @param position 货舱所在位置
 * @param items 对于装箱（可堆叠）货物的 type-num 映射
 * @param unpacks 已拆箱的货物列表
 * */
public record CargoHold(
        long id,
        double restVolume,
        String belongs,
        String position,
        Map<String, Integer> items,
        List<UnpackItem> unpacks) {

    //language=PostgreSQL
    public static final String createTableSql = """
            create table cargo_hold(
                id bigserial primary key,
                rest_volume double precision not null,
                belongs text,
                position text,
                items text not null default '{}',
                unpacks text not null default '[]'
            );
            """;

    public static CargoHold get(SqlClient client, long id) {
        try {
            var row = await(client.preparedQuery("select * from cargo_hold where id = $1").execute(Tuple.of(id)))
                    .iterator().next();
            var mapper = new ObjectMapper();
            return new CargoHold(
                    row.getLong("id"),
                    row.getDouble("rest_volume"),
                    row.getString("belongs"),
                    row.getString("position"),
                    mapper.readValue(row.getString("items"), new TypeReference<>() {
                    }),
                    mapper.readValue(row.getString("unpacks"), new TypeReference<>() {
                    }));
        } catch (Exception e) {
            System.err.println(e.getLocalizedMessage());
            e.printStackTrace();
            return null;
        }
    }
}
