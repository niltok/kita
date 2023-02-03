package ikuyo.utils;

import com.google.common.collect.Sets;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;

public class MsgDiffer {
    int seq = 0;
    String base;
    JsonObject prev = JsonObject.of();

    public MsgDiffer(String base) {
        this.base = base;
    }

    public Buffer next(JsonObject msg) {
        var diff = jsonFlatDiff(prev, msg);
        if (diff.size() == 0) return null;
        seq++;
        prev = msg;
        return JsonObject.of(
                "type", "seq.operate",
                "operate", "diff",
                "target", base,
                "seq", seq,
                "data", diff
        ).toBuffer();
    }

    public Buffer prev() {
        return JsonObject.of(
                "type", "seq.operate",
                "operate", "set",
                "target", base,
                "seq", seq,
                "data", prev
        ).toBuffer();
    }

    public static JsonObject jsonFlatDiff(JsonObject from, JsonObject to) {
        var merged = Sets.union(to.fieldNames(), from.fieldNames());
        var diff = JsonObject.of();
        for (var k : merged) {
            boolean fv = from.containsKey(k);
            Object tv = to.getValue(k);
            if (!fv && tv != null) diff.put(k, tv);
            if (fv && tv == null) diff.putNull(k);
        }
        return diff;
    }
}
