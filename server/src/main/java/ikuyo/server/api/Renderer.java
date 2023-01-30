package ikuyo.server.api;

import ikuyo.api.Star;
import ikuyo.server.renderers.NamedRenderer;
import io.vertx.core.json.JsonObject;

public interface Renderer {
    JsonObject render(Star star);
    default NamedRenderer withName(String name) {
        return new NamedRenderer(name, this);
    }
}
