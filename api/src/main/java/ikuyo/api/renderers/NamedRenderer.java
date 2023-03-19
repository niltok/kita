package ikuyo.api.renderers;

import io.vertx.core.json.JsonObject;

public class NamedRenderer<T> implements Renderer<T> {
    boolean special;
    String name;
    Renderer<T> renderer;
    public NamedRenderer(String name, boolean special, Renderer<T> renderer) {
        this.name = name;
        this.special = special;
        this.renderer = renderer;
    }
    @Override
    public JsonObject render(T ctx) {
        if (!special) return JsonObject.of(name, renderer.render(ctx));
        var res = renderer.render(ctx);
        var map = res.getMap();
        for (var k : map.keySet()) {
            res.put(k, JsonObject.of(name, map.get(k)));
        }
        return res;
    }
}
