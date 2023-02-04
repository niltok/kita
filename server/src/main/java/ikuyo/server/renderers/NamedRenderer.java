package ikuyo.server.renderers;

import ikuyo.api.Star;
import ikuyo.server.api.Renderer;
import io.vertx.core.json.JsonObject;

public class NamedRenderer implements Renderer {
    String name;
    Renderer renderer;
    public NamedRenderer(String name, Renderer renderer) {
        this.name = name;
        this.renderer = renderer;
    }
    @Override
    public JsonObject render(Context ctx) {
        return JsonObject.of(name, renderer.render(ctx));
    }
}
