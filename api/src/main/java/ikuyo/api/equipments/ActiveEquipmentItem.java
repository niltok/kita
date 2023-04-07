package ikuyo.api.equipments;

import ikuyo.api.cargo.AmmoItem;
import ikuyo.api.cargo.CargoStatic;
import ikuyo.api.cargo.UnpackItem;
import ikuyo.api.hooks.HookContext;

import java.util.function.Consumer;

public class ActiveEquipmentItem extends EquipmentItem {
    public final long activeTime;
    public final int ammoMax;
    public final long reloadingTime;
    public final AmmoItem ammoType;
    public final Consumer<HookContext> activeHooker;

    public ActiveEquipmentItem(String displayName,
                               String description,
                               double volume,
                               double unpackVolume,
                               Class<? extends UnpackItem> unpackClass,
                               double hpMax, Consumer<HookContext> hooker,
                               long activeTime, int ammoMax, long reloadingTime, AmmoItem ammoType, Consumer<HookContext> activeHooker) {
        super(displayName, description, volume, unpackVolume, unpackClass, hpMax, hooker);
        this.activeTime = activeTime;
        this.ammoMax = ammoMax;
        this.reloadingTime = reloadingTime;
        this.ammoType = ammoType;
        this.activeHooker = activeHooker;
    }

    public static ActiveEquipmentItem get(String index) {
        try {
            return (ActiveEquipmentItem) CargoStatic.class.getField(index).get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            return null;
        }
    }
}
