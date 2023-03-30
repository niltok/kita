package ikuyo.api;

import com.google.common.collect.ImmutableList;

import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.ArrayList;

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
            start).disable(),
        engine = new TechItem("大统一理论核心引擎",
                "奇迹般的万物之理，通过它你能实现自由高效的能量物质转换", Duration.ofMinutes(1), start),
        engineEnergy = new TechItem("引擎效率提升", "", Duration.ofMinutes(10), engine),
        apple = new TechItem("传说中的苹果", "一颗来自伊甸园的苹果," +
                "也许被偷吃一口之后砸到了牛顿头上最后滚到了乔布斯手里？", Duration.ofSeconds(10), start),
        excalibur = new TechItem("EX咖喱棒",
                "Excalibur是传说中不列颠国王亚瑟王从湖之仙女那得到的圣剑，削铁如泥，可惜时代变啦！", Duration.ofSeconds(20), apple),
        r400 = new TechItem("R400", "传奇的两把经典配枪，二者融合也许能爆发出小女孩和狗一样的震慑力？", Duration.ofMinutes(1), excalibur),
        clusterBomb = new TechItem("集束炸弹", "集束炸弹来啦！坏蛋的必修课，谁的心中不会悄悄藏着一个老六梦呢？", Duration.ofSeconds(90), r400),
        longinus = new TechItem("朗基努斯之枪", "生命之树的部分根须，唯一可以直接穿过A.T.Field并造成有效杀伤的武器", Duration.ofMinutes(10), clusterBomb),
        shieldBattery = new TechItem("", "", Duration.ofSeconds(30), start)
    ;


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
    public boolean isEnable() {
        return enable;
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
            return (TechItem) TechItem.class.getField(index).get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            return null;
        }
    }
    /** 所有已定义的科技项列表 */
    public static final ImmutableList<TechItem> techList;
    /* Auto set TechItem's name */
    static {
        var temp = new ArrayList<TechItem>();
        for (var field : TechItem.class.getFields()) {
            try {
                if (Modifier.isStatic(field.getModifiers()) && field.getType().equals(TechItem.class)) {
                    var tech = (TechItem) field.get(null);
                    temp.add(tech);
                    tech.name = field.getName();
                }
            } catch (Exception ignore) {}
        }
        techList = ImmutableList.copyOf(temp);
    }
}
