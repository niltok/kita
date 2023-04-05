package ikuyo.api.hooks;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE
)
public sealed abstract class HookPoint {
    protected Map<String, Double> hook;
    protected HookPoint() {
        hook = new HashMap<>();
    }
    public HookPoint put(String token, double value) {
        hook.put(token, value);
        return this;
    }
    public String put(double value) {
        var token = UUID.randomUUID().toString();
        put(token, value);
        return token;
    }
    public HookPoint remove(String token) {
        hook.remove(token);
        return this;
    }
    public abstract double reduce();
    public static final class Add extends HookPoint {
        public Add() {
            super();
        }
        @Override
        public double reduce() {
            return hook.values().stream().reduce(0., Double::sum);
        }
    }
    public static final class Mul extends HookPoint {
        public Mul() {
            super();
        }
        @Override
        public double reduce() {
            return hook.values().stream().reduce(1., (acc, x) -> acc * x);
        }
    }
}
