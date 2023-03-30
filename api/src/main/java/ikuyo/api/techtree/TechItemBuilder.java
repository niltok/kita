package ikuyo.api.techtree;

import io.vertx.core.Handler;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class TechItemBuilder {
    private boolean enable = true;
    private String displayName;
    private String description;
    private String type;
    private Function<Long, Duration> cost;
    private Handler<TechItem.CallbackContext> callback = ctx -> {};
    private long maxLevel = 1;
    private List<TechDependency> dependencies = new ArrayList<>();

    private TechItemBuilder() {
    }

    public static TechItemBuilder aTechItem() {
        return new TechItemBuilder();
    }

    public TechItemBuilder withEnable(boolean enable) {
        this.enable = enable;
        return this;
    }

    public TechItemBuilder withDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public TechItemBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public TechItemBuilder withType(String type) {
        this.type = type;
        return this;
    }

    public TechItemBuilder withCost(Function<Long, Duration> cost) {
        this.cost = cost;
        return this;
    }

    public TechItemBuilder withCost(Duration cost) {
        this.cost = l -> cost;
        return this;
    }

    public TechItemBuilder withCallback(Handler<TechItem.CallbackContext> callback) {
        this.callback = callback;
        return this;
    }

    public TechItemBuilder withMaxLevel(long maxLevel) {
        this.maxLevel = maxLevel;
        return this;
    }

    public TechItemBuilder appendDependencies(TechDependency... deps) {
        dependencies.addAll(List.of(deps));
        return this;
    }

    public TechItemBuilder clearDependencies() {
        dependencies.clear();
        return this;
    }

    public TechItem build() {
        return new TechItem(enable,
                type,
                displayName,
                description,
                maxLevel,
                cost,
                callback,
                dependencies.toArray(TechDependency[]::new));
    }
}
