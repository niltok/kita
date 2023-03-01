package ikuyo.utils;

import ch.ethz.globis.phtree.PhTreeMultiMapF;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import ikuyo.api.Drawable;
import ikuyo.api.Position;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.*;

public class MsgDiffer {
    static double CacheRange = 1500;
    String base;
    Map<String, Drawable> prev = new HashMap<>();
    PhTreeMultiMapF<String> tree = PhTreeMultiMapF.create(2);
    Set<String> changed = new HashSet<>();

    public MsgDiffer(String base) {
        this.base = base;
    }

    public void next(JsonObject msg) {
        changed.clear();
        msg.getMap().forEach((k, v) -> {
            changed.add(k);
            if (v == null) {
                var d = prev.get(k);
                tree.remove(new double[]{d.x, d.y}, k.hashCode());
                prev.remove(k);
                return;
            }
            var d = ((JsonObject) v).mapTo(Drawable.class);
            prev.put(k, d);
            tree.put(new double[]{d.x, d.y}, k.hashCode(), k);
        });
    }

    public String query(Position pos, boolean moved, Set<String> cache) {
        Set<String> add = new HashSet<>(), delete = new HashSet<>();
        if (moved) {
            var res = tree.rangeQuery(CacheRange, pos.x, pos.y);
            var set = new HashSet<String>();
            res.forEachRemaining(set::add);
            for (String s : set) {
                if (!cache.contains(s)) add.add(s);
            }
            for (String s : cache) {
                if (!set.contains(s)) delete.add(s);
            }
        }
        for (String s : changed) {
            if (cache.contains(s)) add.add(s);
        }
        cache.addAll(add);
        for (String s : delete) cache.remove(s);
        if (add.isEmpty() && delete.isEmpty()) return null;
        try {
            var outStream = new ByteArrayOutputStream();
            var mapper = new ObjectMapper();
            var writer = mapper.createGenerator(outStream);
            writer.writeStartObject();
            writer.writeStringField("type", "seq.operate");
            writer.writeStringField("target", base);
            writer.writeFieldName("data");
            writer.writeStartObject();
            for (String s : add) {
                writer.writeObjectField(s, prev.get(s));
            }
            for (String s : delete) {
                writer.writeNullField(s);
            }
            writer.writeEndObject();
            writer.writeEndObject();
            writer.close();
            outStream.close();
            return outStream.toString(Charset.defaultCharset());
        } catch (Exception e) {
            System.err.println(e.getLocalizedMessage());
            e.printStackTrace();
            return null;
        }
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
