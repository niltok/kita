package ikuyo.utils;

import com.google.common.collect.Sets;
import io.vertx.await.Async;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.executeblocking.ExecuteBlocking;

import java.util.Objects;

public class MsgDiffer {
    String base;
    JsonObject prev = JsonObject.of();

    public MsgDiffer(String base) {
        this.base = base;
    }

    public Buffer next(JsonObject msg) {
        if (msg.isEmpty()) return null;
        jsonPatchInplace(msg, prev);
//        var diff = jsonDiff(prev, msg);
//        if (diff.isEmpty()) return null;
//        prev = msg;
        return JsonObject.of(
                "type", "seq.operate",
                "target", base,
                "data", msg
        ).toBuffer();
    }

    public Buffer prev() {
        return JsonObject.of(
                "type", "seq.operate",
                "target", base,
                "data", prev
        ).toBuffer();
    }

    public static JsonObject jsonPatchInplace(JsonObject from, JsonObject to) {
        from.getMap().forEach((key, fv) -> {
            var tv = to.getMap().get(key);
            if (fv instanceof JsonArray fa) fv = arrayToObject(fa);
            if (fv instanceof JsonObject fj && tv instanceof JsonObject tj)
                jsonPatchInplace(fj, tj);
            else to.put(key, fv);
        });
        return to;
    }

    public static JsonObject jsonFlatDiff(JsonObject from, JsonObject to) {
        return jsonDiff(from, to, 0);
    }

    public static JsonObject jsonDiff(JsonObject from, JsonObject to, int deep) {
        var merged = Sets.union(to.fieldNames(), from.fieldNames());
        var diff = JsonObject.of();
        for (var k : merged) {
            Object fv = from.getMap().get(k), tv = to.getMap().get(k);
            if (fv == null && tv != null) diff.put(k, tv);
            else if (fv != null && tv == null) diff.putNull(k);
            else if (fv instanceof JsonObject fj && tv instanceof JsonObject tj && deep > 0) {
                var ft = jsonDiff(fj, tj, deep - 1);
                if (!ft.isEmpty()) diff.put(k, ft);
            } else if (fv instanceof JsonArray fa && tv instanceof JsonArray ta && deep > 0) {
                var ft = jsonDiff(arrayToObject(fa), arrayToObject(ta), deep - 1);
                if (!ft.isEmpty()) diff.put(k, ft);
            } else if (!Objects.equals(fv, tv)) diff.put(k, tv);
        }
        return diff;
    }

    public static JsonObject jsonDiff(JsonObject from, JsonObject to) {
        return jsonDiff(from, to, Integer.MAX_VALUE);
    }
    private static JsonObject arrayToObject(JsonArray array) {
        var json = JsonObject.of();
        for (var i = 0; i < array.size(); i++)
            json.put(String.valueOf(i), array.getValue(i));
        return json;
    }
}
