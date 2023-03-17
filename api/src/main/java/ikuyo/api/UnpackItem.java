package ikuyo.api;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * 拆箱的物件，需要保证完整（所有附加状态字段都为默认值）才能装箱成可堆叠物品<br>
 * ！！请保证子类是可序列化的！！ */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public interface UnpackItem {
    /** 作为物品的 type
     * @see CargoItem */
    String getItemType();
    /** 是否完整（所有状态字段是否都为默认值） */
    boolean canPack();
    /** 显示在货舱界面的状态信息 */
    String getItemInfo();
}
