package ikuyo.api.equipments;

import ikuyo.api.UnpackItem;
import ikuyo.api.cargo.AmmoItem;

import java.util.Map;
import java.util.Objects;

public class AbstractWeapon implements UnpackItem {
    public String type;
    public int ammoAmount = 0;
    public double hp;

    public AbstractWeapon(String type) {
        this.type = type;
        var weapon = Objects.requireNonNull(WeaponItem.get(type));
        hp = weapon.hpMax;
    }

    public double getMaxHp() {
        return Objects.requireNonNull(WeaponItem.get(type)).hpMax;
    }

    public AmmoItem getAmmoType() {
        return Objects.requireNonNull(WeaponItem.get(type)).ammoType;
    }

    public double getDamage() {
        return Objects.requireNonNull(WeaponItem.get(type)).damage;
    }

    @Override
    public String getItemType() {
        return type;
    }

    @Override
    public boolean canPack() {
        return hp == getMaxHp();
    }

    @Override
    public void pack(Map<String, Integer> items) {
        var ammo = getAmmoType();
        items.put(type, items.getOrDefault(type, 0) + 1);
        items.put(ammo.type(), items.getOrDefault(ammo.type(), 0) + ammoAmount);
    }

    @Override
    public double packSize() {
        var weapon = Objects.requireNonNull(WeaponItem.get(type));
        return weapon.volume + ammoAmount * weapon.ammoType.volume;
    }

    @Override
    public String getItemInfo() {
        return "";
    }
}
