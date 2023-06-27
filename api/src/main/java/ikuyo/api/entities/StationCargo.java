package ikuyo.api.entities;

import ikuyo.api.cargo.CargoHold;
import ikuyo.api.cargo.CargoItem;
import ikuyo.api.cargo.CargoStatic;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;

import static io.vertx.await.Async.await;

public record StationCargo(int star, int station, int user, CargoHold cargo) {
    //language=PostgreSQL
    public static final String createTableSql = """
            create table station_cargo(
                star int references star not null,
                station int not null,
                "user" int references "user" not null,
                cargo bytea not null,
                primary key (star, station, "user")
            );
            """;

    public static CargoHold adminCargo() {
        var cargo = new CargoHold();
        for (CargoItem item : CargoStatic.itemList()) {
            cargo.put(item.type(), 100);
        }
        return cargo;
    }

    public static CargoHold get(SqlClient client, int star, int station, int user) {
        var rows = await(client.preparedQuery("""
            select cargo from station_cargo where star = $1 and station = $2 and "user" = $3
            """).execute(Tuple.of(star, station, user)));
        if (rows.rowCount() == 0) return adminCargo();
        return CargoHold.fromJson(rows.iterator().next().getBuffer(0));
    }

    public static CargoHold get(SqlClient client, User user) {
        return get(client, user.star(), user.station(), user.id());
    }

    public static void put(SqlClient client, int star, int station, int user, CargoHold cargo) {
        await(client.preparedQuery("""
            insert into station_cargo(star, station, "user", cargo) values ($1, $2, $3, $4)
            on conflict (star, station, "user") do update set cargo = $4
            """).execute(Tuple.of(star, station, user, cargo.toBuffer())));
    }

    public static void put(SqlClient client, User user, CargoHold cargo) {
        put(client, user.star(), user.station(), user.id(), cargo);
    }
}
