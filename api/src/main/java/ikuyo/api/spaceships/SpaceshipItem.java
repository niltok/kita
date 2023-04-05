package ikuyo.api.spaceships;

import ikuyo.api.cargo.CargoItem;
import ikuyo.api.cargo.CargoStatic;
import ikuyo.api.cargo.UnpackItem;

public class SpaceshipItem extends CargoItem {
    public final double hpMax, shieldMax, cargoVolume;
    public final int weaponMax;
    public SpaceshipItem(String displayName,
                         String description,
                         double volume,
                         double unpackVolume,
                         Class<? extends UnpackItem> unpackClass,
                         double hpMax,
                         double shieldMax,
                         double cargoVolume,
                         int weaponMax) {
        super(displayName, description, volume, unpackVolume, unpackClass);
        this.hpMax = hpMax;
        this.shieldMax = shieldMax;
        this.cargoVolume = cargoVolume;
        this.weaponMax = weaponMax;
    }
    public static SpaceshipItem get(String index) {
        try {
            return (SpaceshipItem) CargoStatic.class.getField(index).get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            return null;
        }
    }
}
