package ikuyo.utils;

import ch.ethz.globis.phtree.PhTree;
import com.google.common.collect.Sets;
import ikuyo.api.Drawable;
import ikuyo.api.Position;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;

public class MsgDiffer {
    static final double blockSize = 100;
    static final double cacheRange = 1500;
    static final double blockRange = cacheRange / blockSize;
    String base;
    Map<String, Drawable> prev = new HashMap<>();
    PhTree<Set<String>> tree = PhTree.create(2);
    Set<Changed> changed = new HashSet<>();

    public record LongPair(long x, long y) {}
    record Changed(long x, long y, String s) {}

    public MsgDiffer(String base) {
        this.base = base;
    }

    public long quantize(double a) {
        return (long) Math.floor(a / blockSize);
    }

    public void next(JsonObject msg) {
        changed.clear();
        msg.getMap().forEach((k, v) -> {
            if (v == null) {
                var d = prev.get(k);
                if (d == null) return;
                changed.add(new Changed(quantize(d.x), quantize(d.y), k));
                tree.get(quantize(d.x), quantize(d.y)).remove(k);
                prev.remove(k);
                return;
            }
            var d = ((JsonObject) v).mapTo(Drawable.class);
            changed.add(new Changed(quantize(d.x), quantize(d.y), k));
            prev.put(k, d);
            tree.computeIfAbsent(new long[]{quantize(d.x), quantize(d.y)}, i -> new HashSet<>()).add(k);
        });
    }

    public JsonObject query(int id, Position pos, boolean moved, Set<LongPair> cache) {
        Set<String> add = new HashSet<>(), delete = new HashSet<>();
        Set<LongPair> addBlock = new HashSet<>(), deleteBlock = new HashSet<>();
        if (moved) {
            var res = tree.rangeQuery(blockRange, quantize(pos.x), quantize(pos.y));
            var map = new HashMap<LongPair, Set<String>>();
            while (res.hasNext()) {
                var e = res.nextEntryReuse();
                var k = e.getKey();
                map.put(new LongPair(k[0], k[1]), e.getValue());
            }
            map.forEach((p, set) -> {
                if (cache.contains(p)) return;
                addBlock.add(p);
                for (var k : set) {
                    var d = prev.get(k);
                    if (d == null) System.err.format("dangling drawable %s\n", k);
                    else if (d.user == -1 || d.user == id)
                        add.add(k);
                }
            });
            for (var p : cache) {
                if (map.containsKey(p)) continue;
                deleteBlock.add(p);
                delete.addAll(tree.get(p.x, p.y));
            }
        }
        for (var c : changed) {
            var d = prev.get(c.s);
            var p = new LongPair(c.x, c.y);
            if (d != null && (Math.hypot(pos.x - d.x, pos.y - d.y) <= cacheRange || cache.contains(p))) {
                if (d.user == -1 || d.user == id) add.add(c.s);
            }
            if ((d == null || Math.hypot(pos.x - d.x, pos.y - d.y) > cacheRange) && cache.contains(p)) {
                delete.add(c.s);
            }
        }
        cache.addAll(addBlock);
        for (var s : deleteBlock) cache.remove(s);
        if (add.isEmpty() && delete.isEmpty()) return null;
        return buildDiff(add, delete);
    }

    public JsonObject removeAll(Set<LongPair> cache) {
        var delete = new HashSet<String>();
        for (var p : cache) delete.addAll(tree.get(p.x, p.y));
        return buildDiff(new HashSet<>(), delete);
    }

    public JsonObject buildDiff(Set<String> add, Set<String> delete) {
        var json = JsonObject.of();
        for (String s : add) {
            var d = prev.get(s);
            json.put(s, JsonObject.mapFrom(d));
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
