package ikuyo.api.equipments;

import ikuyo.api.cargo.AmmoItem;
import ikuyo.api.cargo.CargoStatic;
import ikuyo.api.cargo.UnpackItem;
import ikuyo.api.datatypes.Damage;

public class WeaponItem extends ActiveEquipmentItem {
    public final Damage damage;
    public final int ammoMax;
    public final double velocity, collisionRange;
    public final long reloadingTime;
    public final AmmoItem ammoType;

    public WeaponItem(String displayName,
                      String description,
                      double volume,
                      double unpackVolume,
                      Class<? extends UnpackItem> unpackClass,
                      double hpMax,
                      Damage damage,
                      int ammoMax,
                      double velocity,
                      double collisionRange,
                      long activeTime,
                      long reloadingTime,
                      AmmoItem ammoType) {
        super(displayName, description, volume, unpackVolume, unpackClass, hpMax, activeTime);
        this.damage = damage;
        this.velocity = velocity;
        this.collisionRange = collisionRange;
        this.ammoMax = ammoMax;
        this.reloadingTime = reloadingTime;
        this.ammoType = ammoType;
    }
    public static WeaponItem get(String index) {
        try {
            return (WeaponItem) CargoStatic.class.getField(index).get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            return null;
        }
    }
}
