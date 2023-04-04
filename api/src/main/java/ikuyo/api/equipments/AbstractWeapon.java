package ikuyo.api.equipments;

import ikuyo.api.Damage;
import ikuyo.api.UnpackItem;
import ikuyo.api.cargo.AmmoItem;

import java.util.Map;
import java.util.Objects;

public class AbstractWeapon implements UnpackItem {
    public String type;
    public int ammoAmount = 0;
    public long restFireTime = 0;
    public double hp;
    public boolean reloading = false;

    public AbstractWeapon(String type) {
        this.type = type;
        var weapon = Objects.requireNonNull(WeaponItem.get(type));
        hp = weapon.hpMax;
    }

    public AbstractWeapon() {}

    public double getMaxHp() {
        return Objects.requireNonNull(WeaponItem.get(type)).hpMax;
    }

    public AmmoItem getAmmoType() {
        return Objects.requireNonNull(WeaponItem.get(type)).ammoType;
    }

    public Damage getDamage() {
        return Objects.requireNonNull(WeaponItem.get(type)).damage;
    }

    public int getAmmoMax() {
        return Objects.requireNonNull(getInfo()).ammoMax;
    }

    public WeaponItem getInfo() {
        return WeaponItem.get(type);
    }

    public boolean tryFire() {
        if (restFireTime != 0 || ammoAmount == 0) return false;
        restFireTime = Objects.requireNonNull(WeaponItem.get(type)).fireTime;
        ammoAmount--;
        return true;
    }
    /**
     * 装入弹药（只会加装到 {@link AbstractWeapon#getAmmoMax()}）
     * @param provide 提供的弹药总量
     * @return 实际使用的弹药量
     * */
    public int loadAmmo(int provide) {
        var use = Math.min(getAmmoMax() - ammoAmount, provide);
        ammoAmount += use;
        if (use > 0) {
            reloading = true;
            restFireTime = 90;
        }
        return use;
    }

    public void frame() {
        if (restFireTime > 0) restFireTime--;
        if (restFireTime == 0) reloading = false;
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
        if (reloading) return "(%s) Reloading".formatted(getAmmoType().displayName);
        return "(%s) %d / %d".formatted(getAmmoType().displayName, ammoAmount, getAmmoMax());
    }
}