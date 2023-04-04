package ikuyo.api.cargo;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import ikuyo.api.UIElement;
import ikuyo.api.UnpackItem;
import io.vertx.core.json.JsonObject;

import java.util.*;

/**
 * 货舱<br>
 * 可以通过序列化嵌入其他结构，该数据结构上的操作不保证线程安全
 * */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class CargoHold {
    /** 剩余容量 */
    private double restVolume = Double.POSITIVE_INFINITY;
    /** 对于装箱（可堆叠）货物的 type-num 映射<br>！！用于遍历，请不要直接修改！！ */
    public final Map<String, Integer> items = new HashMap<>();
    /** 已拆箱的货物列表<br>！！用于遍历，请不要直接修改！！ */
    public final List<UnpackItem> unpacks = new ArrayList<>();
    public CargoHold() {}
    public CargoHold(double volume) {
        restVolume = volume;
    }
    public double getRestVolume() {
        return restVolume;
    }
    /** 放入物品
     * @param type 物品类型
     * @param num 放入数量
     * @return 0 表示放入成功，1 表示物品不存在，2 表示空间不足
     */
    public int put(String type, int num) {
        var item = CargoItem.get(type);
        if (item == null) return 1;
        if (item.volume * num > restVolume) return 2;
        items.put(type, items.getOrDefault(type, 0) + num);
        restVolume -= item.volume * num;
        return 0;
    }
    /** 放入一个物品
     * @param type 物品类型
     * @return 0 表示放入成功，1 表示物品不存在，2 表示空间不足
     */
    public int put(String type) {
        return put(type, 1);
    }
    /** 放入一个已解包物品
     * @param unpack 物品
     * @return 0 表示放入成功，1 表示物品信息异常，2 表示空间不足
     */
    public int put(UnpackItem unpack) {
        var item = CargoItem.get(unpack.getItemType());
        if (item == null) return 1;
        if (item.unpackVolume > restVolume) return 2;
        unpacks.add(unpack);
        restVolume -= item.unpackVolume;
        return 0;
    }
    /** 打包一个未打包的物品
     * @param index 未打包的物品在 {@link CargoHold#unpacks} 中的下标
     * @return 0 表示打包成功，1 表示物品信息异常，2 表示空间不足，3
     * 表示物品暂时无法打包（{@link UnpackItem#canPack()} 返回 {@code false}）
     */
    public int pack(int index) {
        var unpack = unpacks.get(index);
        if (!unpack.canPack()) return 3;
        var item = CargoItem.get(unpack.getItemType());
        if (item == null) return 1;
        if (unpack.packSize() - item.unpackVolume > restVolume) return 2;
        unpacks.remove(index);
        unpack.pack(items);
        restVolume -= unpack.packSize() - item.unpackVolume;
        return 0;
    }
    /** 解打包物品
     * @param type 将要解包的物品类型
     * @param num 解包数量
     * @return 0 表示解包成功，1 表示物品信息异常，2 表示空间不足，3 表示物品不足
     */
    public int unpack(String type, int num) {
        var item = CargoItem.get(type);
        if (item == null) return 1;
        var totalNum = items.getOrDefault(type, 0);
        if (totalNum < num) return 3;
        var deltaVolume = (item.unpackVolume - item.volume) * num;
        if (deltaVolume > restVolume) return 2;
        restVolume -= deltaVolume;
        if (totalNum == num) items.remove(type);
        else items.put(type, totalNum - num);
        for (var i = 0; i < num; i++) unpacks.add(item.unpack());
        return 0;
    }
    /** 解打包一个物品
     * @param type 将要解包的物品类型
     * @return 0 表示解包成功，1 表示物品信息异常，2 表示空间不足，3 表示物品不足
     */
    public int unpack(String type) {
        return unpack(type, 1);
    }
    /** 取出已打包物品
     * @param type 将要取出的物品类型
     * @param num 物品数量
     * @return 0 表示取出成功，1 表示物品信息异常，2 表示空间不足，3 表示物品不足
     */
    public int take(String type, int num) {
        var item = CargoItem.get(type);
        if (item == null) return 1;
        var totalNum = items.getOrDefault(type, 0);
        if (totalNum < num) return 3;
        var deltaVolume = -item.volume * num;
        if (deltaVolume > restVolume) return 2;
        restVolume -= deltaVolume;
        if (totalNum == num) items.remove(type);
        else items.put(type, totalNum - num);
        return 0;
    }
    /** 取出一个已打包物品
     * @param type 将要取出的物品类型
     * @return 0 表示取出成功，1 表示物品信息异常，2 表示空间不足，3 表示物品不足
     */
    public int take(String type) {
        return take(type, 1);
    }
    /** 取出一个未打包物品
     * @param index 将要取出的物品在 {@link CargoHold#unpacks} 中的下标
     * @return 0 表示取出成功，1 表示物品信息异常，2 表示空间不足
     */
    public int take(int index) {
        var unpack = unpacks.get(index);
        var item = CargoItem.get(unpack.getItemType());
        if (item == null) return 1;
        if (-item.unpackVolume > restVolume) return 2;
        unpacks.remove(index);
        restVolume -= -item.unpackVolume;
        return 0;
    }
    public UIElement renderUI() {
        var uis = new ArrayList<UIElement>();
        items.forEach((type, num) -> {
            var item = Objects.requireNonNull(CargoItem.get(type));
            JsonObject callback = null;
            uis.add(UIElement.labelItem(
                    new UIElement.Text(item.displayName),
                    new UIElement.Text("Num: %d".formatted(num)),
                    callback
            ).appendClass("hover-label").withTitle(item.description));
        });
        unpacks.forEach(unpack -> {
            var item = Objects.requireNonNull(CargoItem.get(unpack.getItemType()));
            JsonObject callback = null;
            uis.add(UIElement.labelItem(
                    new UIElement.Text(item.displayName),
                    new UIElement.Text(unpack.getItemInfo()),
                    callback
            ).appendClass("hover-label").withTitle(item.description));
        });
        return new UIElement("div", uis.toArray(UIElement[]::new));
    }
}
