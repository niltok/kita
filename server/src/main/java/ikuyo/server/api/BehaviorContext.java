package ikuyo.server.api;

import ikuyo.api.Star;
import ikuyo.api.UserKeyInput;

import java.util.Map;

public record BehaviorContext(Star star, Map<Integer, UserKeyInput> userKeyInputs) {
}
