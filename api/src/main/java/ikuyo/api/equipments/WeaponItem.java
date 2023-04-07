package ikuyo.api.equipments;

import ikuyo.api.cargo.AmmoItem;
import ikuyo.api.cargo.CargoStatic;
import ikuyo.api.cargo.UnpackItem;
import ikuyo.api.datatypes.Damage;
import ikuyo.api.hooks.HookContext;

import java.util.function.Consumer;

public class WeaponItem extends ActiveEquipmentItem {
    public final Damage damage;
    public final double velocity, collisionRange;

    public WeaponItem(String displayName,
                      String description,
                      double volume,
                      double unpackVolume,
                      Class<? extends UnpackItem> unpackClass,
                      double hpMax,
                      Consumer<HookContext> hooker,
                      Damage damage,
                      int ammoMax,
                      double velocity,
                      double collisionRange,
                      long activeTime,
                      long reloadingTime,
                      AmmoItem ammoType,
                      Consumer<HookContext> activeHooker) {
        super(displayName,
                description,
                volume,
                unpackVolume,
                unpackClass,
                hpMax,
                hooker,
                activeTime,
                ammoMax,
                reloadingTime,
                ammoType,
                activeHooker);
        this.damage = damage;
        this.velocity = velocity;
        this.collisionRange = collisionRange;
    }
    public static WeaponItem get(String index) {
        try {
            return (WeaponItem) CargoStatic.class.getField(index).get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            return null;
        }
    }
}
