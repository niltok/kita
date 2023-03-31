package ikuyo.api.techtree;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.vertx.core.Handler;

import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;

public non-sealed class TechItem implements TechDependency {
    public record CallbackContext() {}
    private String name;
    public final boolean enable;
    public final String displayName, description, type;
    /** 训练每级所需的时间 */
    public final Function<Long, Duration> cost;
    public final Handler<CallbackContext> callback;
    public final long maxLevel;
    /** 该科技项依赖的科技项 */
    public final ImmutableList<TechLevel> dependencies;
    /** 该科技项在 {@link TechItem} 中被定义时所使用的字段名（系统初始化时依靠反射获取） */
    public String name() {
        return name;
    }
    TechItem(boolean enable, String type,
             String displayName,
             String description,
             long maxLevel,
             Function<Long, Duration> cost,
             Handler<CallbackContext> callback,
             TechDependency... dependencies) {
        this.enable = enable;
        this.type = type;
        this.displayName = displayName;
        this.description = description;
        this.maxLevel = maxLevel;
        this.cost = cost;
        this.callback = callback;
        this.dependencies = ImmutableList.copyOf(Arrays.stream(dependencies).map(dep -> switch (dep) {
            case TechItem techItem -> new TechLevel(techItem, 1);
            case TechLevel techLevel -> techLevel;
        }).toList());
    }
    /** 旧代码兼容层，新 TechItem 请用 {@link TechItemBuilder} 构造 */
    TechItem(String type,
             String displayName,
             String description,
             Duration cost,
             TechDependency... dependencies) {
        this(true, type, displayName, description, 1, l -> cost, ctx -> {}, dependencies);
    }
    public static TechItem get(String index) {
        try {
            return (TechItem) TechStatic.class.getField(index).get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            return null;
        }
    }
    /** 所有已定义的科技项列表 */
    public static final ImmutableList<TechItem> techList;
    /** 所有已定义的科技项按类型分类表 */
    public static final ImmutableMap<String, ImmutableList<TechItem>> techMap;
    /* Auto set TechItem's name */
    static {
        var list = new ArrayList<TechItem>();
        var map = new HashMap<String, List<TechItem>>();
        for (var field : TechStatic.class.getFields()) {
            try {
                if (Modifier.isStatic(field.getModifiers())) {
                    System.out.println(field.getName());
                    var item = field.get(null);
                    if (item instanceof TechItem tech) {
                        System.out.println(field.getName());
                        list.add(tech);
                        map.computeIfAbsent(tech.type, i -> new ArrayList<>()).add(tech);
                        tech.name = field.getName();
                    }
                }
            } catch (Exception ignore) {}
        }
        techList = ImmutableList.copyOf(list);
        techMap = ImmutableMap.copyOf(map.entrySet().stream().map(e ->
                new AbstractMap.SimpleImmutableEntry<>(e.getKey(), ImmutableList.copyOf(e.getValue()))).toList());
    }
}
