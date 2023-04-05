package ikuyo.api.hooks;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE
)
public class UserHook {
    public UserHook() {}
    public HookPoint shieldAddPercent = new HookPoint.Add();
    public double getShield(double base) {
        return base * (1 + shieldAddPercent.reduce());
    }
}
