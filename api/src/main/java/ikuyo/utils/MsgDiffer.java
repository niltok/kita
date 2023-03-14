package ikuyo.utils;

import ch.ethz.globis.phtree.PhTreeMultiMapF;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import ikuyo.api.Drawable;
import ikuyo.api.Position;
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
                if (d == null) return;
                tree.remove(new double[]{d.x, d.y}, k.hashCode());
                prev.remove(k);
                return;
            }
            var d = ((JsonObject) v).mapTo(Drawable.class);
            prev.put(k, d);
            tree.put(new double[]{d.x, d.y}, k.hashCode(), k);
        });
    }

    public JsonObject query(Position pos, boolean moved, Set<String> cache) {
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
        return buildDiff(add, delete);
    }

    public JsonObject buildDiff(Set<String> add, Set<String> delete) {
        var json = JsonObject.of();
        for (String s : add) {
            json.put(s, JsonObject.mapFrom(prev.get(s)));
        }
        for (String s : delete) {
            json.putNull(s);
        }
        return json;
    }

    public static JsonObject jsonPatchInplace(JsonObject from, JsonObject to) {
        from.getMap().forEach((key, fv) -> {
            var tv = to.getMap().get(key);
            if (fv instanceof JsonArray fa) fv = jsonArrayToObject(fa);
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
            Object fv = from.getValue(k), tv = to.getValue(k);
            if (fv == null && tv != null) diff.put(k, tv);
            else if (fv != null && tv == null) diff.putNull(k);
            else if (fv instanceof JsonObject fj && tv instanceof JsonObject tj && deep > 0) {
                var ft = jsonDiff(fj, tj, deep - 1);
                if (!ft.isEmpty()) diff.put(k, ft);
            } else if (fv instanceof JsonArray fa && tv instanceof JsonArray ta && deep > 0) {
                var ft = jsonDiff(jsonArrayToObject(fa), jsonArrayToObject(ta), deep - 1);
                if (!ft.isEmpty()) diff.put(k, ft);
            } else if (!Objects.equals(fv, tv)) diff.put(k, tv);
        }
        return diff;
    }

    public static JsonObject jsonDiff(JsonObject from, JsonObject to) {
        return jsonDiff(from, to, Integer.MAX_VALUE);
    }
    private static JsonObject jsonArrayToObject(JsonArray array) {
        var json = JsonObject.of();
        for (var i = 0; i < array.size(); i++)
            json.put(String.valueOf(i), array.getValue(i));
        return json;
    }
}
