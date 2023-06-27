package ikuyo.api.datatypes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import ikuyo.utils.DataStatic;
import ikuyo.utils.Position;
import io.vertx.core.buffer.Buffer;

import java.io.IOException;
import java.util.List;

public class Station {
    public static final double dockDist = 600 / Drawable.scaling;
    public Position pos;
    public Station() {}
    public Station(Position pos) {
        this.pos = pos;
    }
    public Station(double x, double y) {
        this(new Position(x, y));
    }

    public static List<Station> fromJson(Buffer buffer) {
        try {
            return DataStatic.mapper.readValue(DataStatic.gzipDecode(buffer), new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Buffer toBuffer(List<Station> stations) {
        try {
            return DataStatic.gzipEncode(DataStatic.mapper.writeValueAsBytes(stations));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
