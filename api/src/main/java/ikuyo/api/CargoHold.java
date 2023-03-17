package ikuyo.api;

import java.util.List;
import java.util.Map;

/**
 * 货舱 (WIP)
 * @param belongs 货舱拥有者
 * @param position 货舱所在位置
 * @param items 对于装箱（可堆叠）货物的 type-num 映射
 * @param unpacks 已拆箱的货物列表
 * */
public record CargoHold(
        long id,
        double restVolume,
        String belongs, // TODO
        String position, // TODO
        Map<String, Integer> items,
        List<UnpackItem> unpacks) {
}
