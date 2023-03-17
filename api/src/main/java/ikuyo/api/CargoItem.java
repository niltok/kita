package ikuyo.api;

import com.google.common.collect.ImmutableList;

import java.lang.reflect.Modifier;
import java.util.ArrayList;

/** 货物类型 */
public class CargoItem {
    public static final CargoItem
            soil = new CargoItem("泥土", "岩石碎裂之后的产物", 10),
            stone = new CargoItem("石头", "星球深处的岩浆凝固后的产物", 10);

    /*=====================================================================*/
    private String type;
    public final String displayName, description;
    public final double volume, unpackVolume;
    public CargoItem(String displayName, String description, double volume, double unpackVolume) {
        this.displayName = displayName;
        this.description = description;
        this.volume = volume;
        this.unpackVolume = unpackVolume;
    }
    public CargoItem(String displayName, String description, int volume) {
        this(displayName, description, volume, -1);
    }
    public String type() {
        return type;
    }
    public static CargoItem get(String index) {
        try {
            return (CargoItem) CargoItem.class.getField(index).get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            return null;
        }
    }
    /** 所有货物类型列表 */
    public static final ImmutableList<CargoItem> itemList;
    /* Auto set CargoItem's name */
    static {
        var temp = new ArrayList<CargoItem>();
        for (var field : CargoItem.class.getFields()) {
            try {
                if (Modifier.isStatic(field.getModifiers()) && field.getType().equals(CargoItem.class)) {
                    var item = (CargoItem) field.get(null);
                    temp.add(item);
                    item.type = field.getName();
                }
            } catch (Exception ignore) {}
        }
        itemList = ImmutableList.copyOf(temp);
    }
}
