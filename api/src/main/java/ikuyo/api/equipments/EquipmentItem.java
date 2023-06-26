package ikuyo.api.equipments;

import ikuyo.api.cargo.CargoItem;
import ikuyo.api.cargo.CargoStatic;
import ikuyo.api.cargo.UnpackItem;
import ikuyo.api.hooks.HookContext;

import java.util.function.Consumer;

public class EquipmentItem extends CargoItem {
    public final double hpMax;
    public final Consumer<HookContext> hooker;
    public EquipmentItem(String displayName,
                         String description,
                         double volume,
                         double unpackVolume,
                         Class<? extends UnpackItem> unpackClass,
                         double hpMax, Consumer<HookContext> hooker) {
        super(displayName, description, volume, unpackVolume, unpackClass);
        this.hpMax = hpMax;
        this.hooker = hooker;
    }
    public Equipment unpack() {
        return (Equipment) super.unpack();
    }
    public static EquipmentItem get(String index) {
        try {
            return (EquipmentItem) CargoStatic.class.getField(index).get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            return null;
        }
    }
}
