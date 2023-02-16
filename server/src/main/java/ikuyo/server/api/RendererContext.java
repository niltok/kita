package ikuyo.server.api;

import ikuyo.api.Star;
import io.vertx.core.Vertx;

public record RendererContext(Vertx vertx, Star star, UpdatedContext updated) {
}
