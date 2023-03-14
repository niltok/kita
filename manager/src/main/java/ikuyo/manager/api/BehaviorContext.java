package ikuyo.manager.api;

import io.vertx.core.json.JsonObject;

public record BehaviorContext(Integer id, JsonObject event, CommonContext context) {
}
