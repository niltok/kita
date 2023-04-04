package ikuyo.api.spaceships;

import ikuyo.api.UnpackItem;
import ikuyo.api.cargo.CargoHold;
import ikuyo.api.equipments.AbstractWeapon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AbstractSpaceship implements UnpackItem {
    public String type;
    public double hp, shield;
    public CargoHold cargoHold;
    public List<AbstractWeapon> weapons;
    public int currentWeapon = 0;
    public AbstractSpaceship(String type) {
        this.type = type;
        var ship = Objects.requireNonNull(SpaceshipItem.get(type));
        hp = getMaxHp();
        shield = getMaxShield();
        cargoHold = new CargoHold(ship.cargoVolume);
        weapons = new ArrayList<>(getMaxWeapon());
    }

    public AbstractSpaceship() {}

    public AbstractWeapon getCurrentWeapon() {
        return weapons.get(currentWeapon);
    }

    public double getMaxHp() {
        return Objects.requireNonNull(SpaceshipItem.get(type)).hpMax;
    }

    public int getMaxWeapon() {
        return Objects.requireNonNull(SpaceshipItem.get(type)).weaponMax;
    }

    public double getMaxShield() {
        return Objects.requireNonNull(SpaceshipItem.get(type)).shieldMax;
    }

    public void inflict(double shieldDamage, double hpDamage) {
        this.shield = Math.max(this.shield - shieldDamage, 0);
        this.hp = Math.max(this.hp - hpDamage, 0);
    }

    public boolean tryFire() {
        var weapon = getCurrentWeapon();
        var ammo = weapon.getAmmoType().type();
        if (weapon.ammoAmount == 0)
            cargoHold.take(ammo, weapon.reloadAmmo(cargoHold.items.getOrDefault(ammo, 0)));
        return weapon.tryFire();
    }

    @Override
    public String getItemType() {
        return type;
    }

    @Override
    public boolean canPack() {
        return hp >= getMaxHp() && shield >= getMaxShield()
                && weapons.stream().allMatch(w -> w == null || w.canPack());
    }

    @Override
    public void pack(Map<String, Integer> items) {
        items.put(type, items.getOrDefault(type, 0) + 1);
        for (var w : weapons) w.pack(items);
    }

    @Override
    public double packSize() {
        return Objects.requireNonNull(SpaceshipItem.get(type)).volume
                + weapons.stream().mapToDouble(w -> w == null ? 0 : w.packSize()).sum();
    }

    @Override
    public String getItemInfo() {
        return "";
    }
}
