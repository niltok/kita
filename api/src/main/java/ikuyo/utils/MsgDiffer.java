package ikuyo.utils;

import com.google.common.collect.Sets;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

import java.util.Objects;

public class MsgDiffer {
    String base;
    JsonObject prev = JsonObject.of();

    public MsgDiffer(String base) {
        this.base = base;
    }

    public Buffer next(JsonObject msg) {
        var diff = jsonDiff(prev, msg);
        if (diff.isEmpty()) return null;
        prev = msg;
        return JsonObject.of(
                "type", "seq.operate",
                "target", base,
                "data", diff
        ).toBuffer();
    }

    public Buffer prev() {
        return JsonObject.of(
                "type", "seq.operate",
                "target", base,
                "data", prev
        ).toBuffer();
    }

    public static JsonObject jsonFlatDiff(JsonObject from, JsonObject to) {
        return jsonDiff(from, to, 0);
    }

    public static JsonObject jsonDiff(JsonObject from, JsonObject to, int deep) {
        var merged = Sets.union(to.fieldNames(), from.fieldNames());
        var diff = JsonObject.of();
        for (var k : merged) {
            Object fv = from.getValue(k), tv = to.getValue(k);
            if (fv == null && tv != null) diff.put(k, tv);
            else if (fv != null && tv == null) diff.putNull(k);
            else if (fv instanceof JsonObject fj && tv instanceof JsonObject tj && deep > 0) {
                var ft = jsonDiff(fj, tj, deep - 1);
                if (!ft.isEmpty()) diff.put(k, ft);
            } else if (!Objects.equals(fv, tv)) diff.put(k, tv);
        }
        return diff;
    }

    public static JsonObject jsonDiff(JsonObject from, JsonObject to) {
        return jsonDiff(from, to, Integer.MAX_VALUE);
    }
}
