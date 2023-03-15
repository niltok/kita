package ikuyo.api;

import com.google.common.collect.ImmutableList;

import java.lang.reflect.Modifier;
import java.time.Duration;

public class TechItem {
    /*====================================================================
     科技树的静态数据定义
     ！！请不要写出依赖环！！
     TechItem.name 将会在系统初始化的时候被自动指定为其在 TechItem 中的字段名
     无需手动设置 TechItem
     ====================================================================*/
    public static final TechItem
            start = new TechItem("开始", "一切的起点", Duration.ZERO),
            fireBase = new TechItem("射击学基础", "学习如何使用武器", Duration.ofSeconds(5),
                    start).disable();


    /*====================================================================
     TechItem class definition
     ====================================================================*/
    private boolean enable = true;
    private String name;
    public final String displayName, description;
    /** 训练所需的时间 */
    public final Duration cost;
    /** 该科技项依赖的科技项 */
    public final ImmutableList<TechItem> dependencies;
    TechItem disable() {
        this.enable = false;
        return this;
    }
    public boolean isDisable() {
        return !enable;
    }
    /** 该科技项在 {@link TechItem} 中被定义时所使用的字段名（系统初始化时依靠反射获取） */
    public String name() {
        return name;
    }
    TechItem(String displayName, String description, Duration cost, TechItem... dependencies) {
        this.displayName = displayName;
        this.description = description;
        this.cost = cost;
        this.dependencies = ImmutableList.copyOf(dependencies);
    }
    public static TechItem get(String index) {
        try {
            return (TechItem) TechTree.class.getField(index).get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            return null;
        }
    }
    /* Auto set TechItem's name */
    static {
        for (var field : TechItem.class.getFields()) {
            try {
                if (Modifier.isStatic(field.getModifiers()) && field.getType().equals(TechItem.class)) {
                    ((TechItem) field.get(null)).name = field.getName();
                }
            } catch (Exception ignore) {}
        }
    }
}
