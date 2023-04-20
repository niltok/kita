package ikuyo.api.techtree;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import ikuyo.utils.ItemUtils;
import io.vertx.core.json.Json;

import java.time.Duration;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 科技树的静态数据定义<br>
 * ！！请不要写出依赖环！！<br>
 * TechItem.name 将会在系统初始化的时候被自动指定为其在 TechItem 中的字段名<br>
 * 无需手动设置 TechItem
 * */
@ItemUtils.ItemTarget(TechItem.class)
public class TechStatic {
    public static final TechItem
            start = new TechItem("", "开始", "一切的起点", Duration.ZERO),
            fireBase = new TechItem("", "射击学基础", "学习如何使用武器", Duration.ofSeconds(5),
                    start),
            engine = new TechItem("", "大统一理论核心引擎",
                    "奇迹般的万物之理，通过它你能实现自由高效的能量物质转换", Duration.ofMinutes(1), start),
            engineEnergy = new TechItem("", "引擎效率提升", "", Duration.ofMinutes(10), engine),
            apple = new TechItem("", "传说中的苹果", "一颗来自伊甸园的苹果," +
                    "也许被偷吃一口之后砸到了牛顿头上最后滚到了乔布斯手里？", Duration.ofSeconds(10), start),
            excalibur = new TechItem("", "EX咖喱棒",
                    "Excalibur是传说中不列颠国王亚瑟王从湖之仙女那得到的圣剑，削铁如泥，可惜时代变啦！", Duration.ofSeconds(20), apple),
            r400 = new TechItem("", "R400", "传奇的两把经典配枪，二者融合也许能爆发出小女孩和狗一样的震慑力？", Duration.ofMinutes(1), excalibur),
            clusterBomb = new TechItem("", "集束炸弹", "集束炸弹来啦！坏蛋的必修课，谁的心中不会悄悄藏着一个老六梦呢？", Duration.ofSeconds(90), r400),
            longinus = new TechItem("", "朗基努斯之枪", "生命之树的部分根须，唯一可以直接穿过A.T.Field并造成有效杀伤的武器", Duration.ofMinutes(10), clusterBomb),
            shieldBattery = new TechItem("", "", "", Duration.ofSeconds(30), start)
    ;
    /** 所有已定义的科技项列表 */
    @ItemUtils.ItemList
    private static ImmutableList<TechItem> techList;
    public static ImmutableList<TechItem> techList() {
        return techList;
    }
    /** 所有已定义的科技项按类型分类表 */
    public static final ImmutableMap<String, ImmutableList<TechItem>> techMap;
    private Json des;

    /* Auto set TechItem's name */
    static {
        ItemUtils.setFieldName(TechStatic.class);
        var map = new HashMap<String, List<TechItem>>();
        techList.forEach(tech -> map.computeIfAbsent(tech.type, i -> new ArrayList<>()).add(tech));
        techMap = ImmutableMap.copyOf(map.entrySet().stream().map(e ->
                new AbstractMap.SimpleImmutableEntry<>(e.getKey(), ImmutableList.copyOf(e.getValue()))).toList());
    }
}
