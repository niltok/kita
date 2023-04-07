package ikuyo.api.hooks;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.HashMap;
import java.util.Map;

@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE
)
public abstract class AbstractHook {
    static {
        try {
            Class.forName(HookStatic.class.getName());
        } catch (ClassNotFoundException ignore) {}
    }

    private final Map<String, Map<String, HookData>> hookDatas = new HashMap<>();
    public AbstractHook() {}
    public double reduce(HookPoint hook) {
        return hook.reduce(hookDatas.get(hook.name()));
    }
    public HookToken put(HookPoint hookPoint, HookData data) {
        return new HookToken(hookPoint.name(),
                hookPoint.put(hookDatas.computeIfAbsent(hookPoint.name(), i -> new HashMap<>()), data));
    }
    public HookToken put(HookPoint hookPoint, double value) {
        return put(hookPoint, new HookData(value));
    }
    public AbstractHook remove(HookToken token) {
        var map = hookDatas.get(token.hookPoint);
        if (map != null) map.remove(token.hookId);
        return this;
    }
}
