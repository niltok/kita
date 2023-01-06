package ikuyo.api;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

public class StarInfo {
    public Block[] blocks;

    public JsonObject toJson() {
        return JsonObject.mapFrom(this);
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

    public static StarInfo genStar(int seed) {
        var info = new StarInfo();
        info.blocks = new Block[100];
        for (var i = 0; i < 100; i++) {
            info.blocks[i] = new Block.Normal();
            info.blocks[i].id = 0;
        }
        return info;
    }

    public static StarInfo fromJson(String str) {
        return fromJson(new JsonObject(str));
    }

    public static StarInfo fromJson(Buffer buffer) {
        return fromJson(new JsonObject(buffer));
    }

    public static StarInfo fromJson(JsonObject json) {
        return json.mapTo(StarInfo.class);
    }
}
