package ikuyo.manager.api;

import io.vertx.core.json.JsonObject;

public record BehaviorArgContext(Integer id, JsonObject event, BehaviorContext context) {
}
