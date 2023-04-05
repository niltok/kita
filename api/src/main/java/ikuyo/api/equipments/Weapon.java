package ikuyo.api.equipments;

import ikuyo.api.cargo.AmmoItem;
import ikuyo.api.datatypes.Damage;
import ikuyo.api.spaceships.Spaceship;

import java.util.Map;
import java.util.Objects;

public class Weapon extends ActiveEquipment {
    public int ammoAmount = 0;
    public boolean reloading = false;

    public Weapon(String type) {
        this.type = type;
        var weapon = Objects.requireNonNull(ActiveEquipmentItem.get(type));
        hp = weapon.hpMax;
    }

    public Weapon() {}

    public AmmoItem getAmmoType() {
        return getInfo().ammoType;
    }

    public Damage getDamage() {
        return getInfo().damage;
    }

    public int getAmmoMax() {
        return getInfo().ammoMax;
    }

    public long getReloadingTime() {
        return getInfo().reloadingTime;
    }

    public WeaponItem getInfo() {
        return Objects.requireNonNull(WeaponItem.get(type));
    }

    /**
     * 装入弹药（只会加装到 {@link Weapon#getAmmoMax()}）
     * @param provide 提供的弹药总量
     * @return 实际使用的弹药量
     * */
    public int reloadAmmo(int provide) {
        var use = Math.min(getAmmoMax() - ammoAmount, provide);
        ammoAmount += use;
        if (use > 0) {
            reloading = true;
            restActiveTime = getReloadingTime();
        }
        return use;
    }

    @Override
    public boolean canEnable() {
        if (super.canEnable()) {
            assert spaceship != null;
            int i = spaceship.weapons.indexOf(this);
            return super.canEnable() && i >= 0 && i < spaceship.getMaxWeapon();
        }
        return false;
    }

    @Override
    public boolean tryActive() {
        if (restActiveTime != 0 || ammoAmount == 0) return false;
        restActiveTime = getActiveTime();
        ammoAmount--;
        return true;
    }

    @Override
    public void frame() {
        super.frame();
        if (restActiveTime == 0) reloading = false;
    }

    @Override
    public void unequip() {
        if (spaceship != null) spaceship.weapons.remove(this);
        super.unequip();
    }

    @Override
    public Weapon equip(Spaceship spaceship) {
        super.equip(spaceship);
        spaceship.weapons.add(this);
        return this;
    }

    @Override
    public boolean canPack() {
        return super.canPack();
    }

    @Override
    public void pack(Map<String, Integer> items) {
        super.pack(items);
        var ammo = getAmmoType();
        items.put(type, items.getOrDefault(type, 0) + 1);
        items.put(ammo.type(), items.getOrDefault(ammo.type(), 0) + ammoAmount);
    }

    @Override
    public double packSize() {
        return super.packSize() + ammoAmount * getAmmoType().volume;
    }

    @Override
    public String getItemInfo() {
        if (!enable) return "Disabled";
        if (reloading) return "%s Reloading".formatted(getAmmoType().displayName);
        return "%s: %d / %d".formatted(getAmmoType().displayName, ammoAmount, getAmmoMax());
    }
}
