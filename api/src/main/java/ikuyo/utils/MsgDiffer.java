package ikuyo.utils;

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
                "type", "state.dispatch",
                "action", "seqState/diffSeq",
                "payload", JsonObject.of(
                    "target", base,
                    "seq", seq,
                    "diff", diff
                )
        ).toBuffer();
    }

    public Buffer prev() {
        return JsonObject.of(
                "type", "state.dispatch",
                "action", "seqState/setSeq",
                "payload", JsonObject.of(
                    "target", base,
                    "seq", seq,
                    "data", prev
                )
        ).toBuffer();
    }

    public static JsonObject jsonFlatDiff(JsonObject from, JsonObject to) {
        var merged = to.mergeIn(from);
        var diff = new HashMap<String, Object>();
        for (var kv : merged) {
            var k = kv.getKey();
            Object fv = from.getValue(k), tv = to.getValue(k);
            if (fv == null && tv != null) diff.put(k, kv.getValue());
            if (fv != null && tv == null) diff.put(k, null);
        }
        return new JsonObject(diff);
    }
}
