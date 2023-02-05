package ikuyo.api.renderers;

import io.vertx.core.json.JsonObject;

public class NamedRenderer<T> implements Renderer<T> {
    String name;
    Renderer<T> renderer;
    public NamedRenderer(String name, Renderer<T> renderer) {
        this.name = name;
        this.renderer = renderer;
    }
    @Override
    public JsonObject render(T ctx) {
        return JsonObject.of(name, renderer.render(ctx));
    }
}
