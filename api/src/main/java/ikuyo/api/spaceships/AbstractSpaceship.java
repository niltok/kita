package ikuyo.api.spaceships;

import ikuyo.api.UnpackItem;
import ikuyo.api.cargo.CargoHold;
import ikuyo.api.equipments.AbstractWeapon;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public class AbstractSpaceship implements UnpackItem {
    public String type;
    public double hp, shield;
    public CargoHold cargoHold;
    public AbstractWeapon[] weapons;
    public AbstractSpaceship(String type) {
        this.type = type;
        var ship = Objects.requireNonNull(SpaceshipItem.get(type));
        hp = getMaxHp();
        shield = getMaxShield();
        cargoHold = new CargoHold(ship.cargoVolume);
        weapons = new AbstractWeapon[ship.weaponMax];
    }

    public double getMaxHp() {
        return Objects.requireNonNull(SpaceshipItem.get(type)).hpMax;
    }

    public double getMaxShield() {
        return Objects.requireNonNull(SpaceshipItem.get(type)).shieldMax;
    }

    @Override
    public String getItemType() {
        return type;
    }

    @Override
    public boolean canPack() {
        return hp >= getMaxHp() && shield >= getMaxShield()
                && Arrays.stream(weapons).allMatch(w -> w == null || w.canPack());
    }

    @Override
    public void pack(Map<String, Integer> items) {
        items.put(type, items.getOrDefault(type, 0) + 1);
        for (var w : weapons) w.pack(items);
    }

    @Override
    public double packSize() {
        return Objects.requireNonNull(SpaceshipItem.get(type)).volume
                + Arrays.stream(weapons).mapToDouble(w -> w == null ? 0 : w.packSize()).sum();
    }

    @Override
    public String getItemInfo() {
        return "";
    }
}
