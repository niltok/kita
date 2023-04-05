package ikuyo.api.equipments;

import ikuyo.api.cargo.CargoStatic;
import ikuyo.api.cargo.UnpackItem;

public class ActiveEquipmentItem extends EquipmentItem {
    public final long activeTime;

    public ActiveEquipmentItem(String displayName,
                               String description,
                               double volume,
                               double unpackVolume,
                               Class<? extends UnpackItem> unpackClass,
                               double hpMax,
                               long activeTime) {
        super(displayName, description, volume, unpackVolume, unpackClass, hpMax);
        this.activeTime = activeTime;
    }

    public static ActiveEquipmentItem get(String index) {
        try {
            return (ActiveEquipmentItem) CargoStatic.class.getField(index).get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            return null;
        }
    }
}
