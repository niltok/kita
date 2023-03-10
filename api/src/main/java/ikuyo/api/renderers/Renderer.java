package ikuyo.api.renderers;

import io.vertx.core.json.JsonObject;

public interface Renderer<T> {
    JsonObject render(T context);
    default NamedRenderer<T> withName(String name) {
        return new NamedRenderer<>(name, this);
    }
}
