package ikuyo.api.equipments;

import com.google.common.collect.ImmutableList;
import ikuyo.api.UnpackItem;
import ikuyo.api.cargo.CargoItem;
import ikuyo.api.cargo.CargoStatic;

import java.lang.reflect.Modifier;
import java.util.ArrayList;

public abstract class EquipmentItem extends CargoItem {
    public final double hpMax;
    public EquipmentItem(String displayName,
                         String description,
                         double volume,
                         double unpackVolume,
                         Class<? extends UnpackItem> unpackClass,
                         double hpMax) {
        super(displayName, description, volume, unpackVolume, unpackClass);
        this.hpMax = hpMax;
    }
    public static EquipmentItem get(String index) {
        try {
            return (EquipmentItem) CargoStatic.class.getField(index).get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            return null;
        }
    }
    /** 所有装备类型列表 */
    public static final ImmutableList<EquipmentItem> itemList;
    /* Auto collect itemList */
    static {
        var temp = new ArrayList<EquipmentItem>();
        for (var field : CargoStatic.class.getFields()) {
            try {
                if (Modifier.isStatic(field.getModifiers())) {
                    var obj = field.get(null);
                    if (obj instanceof EquipmentItem item) {
                        temp.add(item);
                    }
                }
            } catch (Exception ignore) {}
        }
        itemList = ImmutableList.copyOf(temp);
    }
}
