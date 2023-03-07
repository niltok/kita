package ikuyo.server.api;

import ikuyo.api.Star;
import ikuyo.api.User;
import ikuyo.api.UserKeyInput;

import java.util.Map;

public record BehaviorContext(
        Map<Integer, UserKeyInput> userKeyInputs,
        CommonContext common,
        UpdatedContext updated
) {
}
