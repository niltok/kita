package ikuyo.api.hooks;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import ikuyo.utils.ItemUtils;

import java.util.Map;
import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE
)
public sealed abstract class HookPoint {
    @ItemUtils.ItemName
    private String name;
    protected HookPoint() {}
    public String name() {
        return name;
    }
    public HookPoint put(Map<String, HookData> hook, String token, HookData value) {
        hook.put(token, value);
        return this;
    }
    public String put(Map<String, HookData> hook, HookData value) {
        var token = UUID.randomUUID().toString();
        put(hook, token, value);
        return token;
    }
    public HookPoint remove(Map<String, HookData> hook, String token) {
        hook.remove(token);
        return this;
    }
    public abstract double reduce(Map<String, HookData> hook);
    public static final class Add extends HookPoint {
        public Add() {
            super();
        }
        @Override
        public double reduce(Map<String, HookData> hook) {
            if (hook == null) return 0;
            return hook.values().stream().reduce(0., (acc, x) -> acc + x.value, Double::sum);
        }
    }
    public static final class Mul extends HookPoint {
        public Mul() {
            super();
        }
        @Override
        public double reduce(Map<String, HookData> hook) {
            if (hook == null) return 1;
            return hook.values().stream().reduce(1., (acc, x) -> acc * x.value, (a, b) -> a * b);
        }
    }
}
