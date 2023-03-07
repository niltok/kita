package ikuyo.server.api;

import ikuyo.api.Star;
import ikuyo.api.User;
import io.vertx.core.Vertx;

import java.util.Map;

public record RendererContext(
        Vertx vertx,
        CommonContext common,
        UpdatedContext updated
) {
}
