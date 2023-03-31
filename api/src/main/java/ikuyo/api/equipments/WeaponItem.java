package ikuyo.api.equipments;

import ikuyo.api.Damage;
import ikuyo.api.UnpackItem;
import ikuyo.api.cargo.AmmoItem;
import ikuyo.api.cargo.CargoStatic;

public class WeaponItem extends EquipmentItem {
    public final Damage damage;
    public final int ammoMax;
    public final double velocity, collisionRange;
    public final long fireTime;
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
                      long fireTime,
                      AmmoItem ammoType) {
        super(displayName, description, volume, unpackVolume, unpackClass, hpMax);
        this.damage = damage;
        this.velocity = velocity;
        this.collisionRange = collisionRange;
        this.ammoMax = ammoMax;
        this.fireTime = fireTime;
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
