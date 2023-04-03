package ikuyo.manager.api;

import ikuyo.utils.Property;
import io.vertx.core.json.JsonObject;

import java.util.Map;

public record BehaviorContext(Integer id, JsonObject event, CommonContext context, Property<Boolean> forward) {
    public BehaviorContext(Integer id, JsonObject event, CommonContext context) {
        this(id, event, context, new Property<>(true));
    }
    public Map<Integer, UserState> getStates() {
        return context.userState();
    }
    public UserState getState(int id) {
        return  getStates().get(id);
    }
    public UpdatedContext updated() {
        return context.updated();
    }
}
