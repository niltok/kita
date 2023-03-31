package ikuyo.api.equipments;

import ikuyo.api.UnpackItem;
import ikuyo.api.cargo.CargoItem;
import ikuyo.api.cargo.CargoStatic;

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
}
