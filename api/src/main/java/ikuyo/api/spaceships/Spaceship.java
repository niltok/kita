package ikuyo.api.spaceships;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import ikuyo.api.cargo.CargoHold;
import ikuyo.api.cargo.UnpackItem;
import ikuyo.api.datatypes.UserInfo;
import ikuyo.api.equipments.ActiveEquipment;
import ikuyo.api.equipments.Equipment;
import ikuyo.api.equipments.Weapon;
import ikuyo.api.hooks.SpaceshipHook;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Spaceship implements UnpackItem {
    public String type;
    public double hp, shield;
    public CargoHold cargoHold;
    @JsonManagedReference
    public List<Weapon> weapons;
    @JsonManagedReference
    public List<Equipment> passiveEquipments;
    @JsonManagedReference
    public List<ActiveEquipment> activeEquipments;
    public int currentWeapon = 0;
    @JsonBackReference
    @Nullable
    public UserInfo user;
    @JsonManagedReference
    public SpaceshipHook hooks = new SpaceshipHook(this);

    public Spaceship(String type) {
        this.type = type;
        var ship = Objects.requireNonNull(SpaceshipItem.get(type));
        hp = getMaxHp();
        shield = getMaxShield();
        cargoHold = new CargoHold(ship.cargoVolume);
        weapons = new ArrayList<>(getWeaponMax());
        passiveEquipments = new ArrayList<>(getPassiveEquipmentMax());
        activeEquipments = new ArrayList<>(getActiveEquipmentMax());
    }

    public Spaceship() {}

    public Weapon getCurrentWeapon() {
        return weapons.get(currentWeapon);
    }

    public double getMaxHp() {
        return getInfo().hpMax;
    }

    public int getWeaponMax() {
        return getInfo().weaponMax;
    }

    public int getActiveEquipmentMax() {
        return getInfo().activeEquipmentMax;
    }

    public int getPassiveEquipmentMax() {
        return getInfo().passiveEquipmentMax;
    }

    public double getMaxShield() {
        return hooks.getShield();
    }

    public SpaceshipItem getInfo() {
        return Objects.requireNonNull(SpaceshipItem.get(type));
    }

    public void undeploy() {
        if (user != null) user.spaceship = null;
        user = null;
    }

    public void deploy(UserInfo user) {
        undeploy();
        if (user.spaceship != null) user.spaceship.undeploy();
        user.spaceship = this;
        this.user = user;
    }

    public void inflict(double shieldDamage, double hpDamage) {
        this.shield = Math.max(this.shield - shieldDamage, 0);
        this.hp = Math.max(this.hp - hpDamage, 0);
    }

    public boolean tryFire() {
        var weapon = getCurrentWeapon();
        var ammo = weapon.getAmmoType();
        var flag = weapon.tryActive();
        if (!flag && ammo != null && weapon.ammoAmount == 0)
            cargoHold.take(ammo.type(), weapon.reloadAmmo(cargoHold.items.getOrDefault(ammo.type(), 0)));
        return flag;
    }

    public boolean frame() {
        boolean update = false;
        for (var weapon : weapons) {
            if (weapon.restActiveTime > 0) update = true;
            weapon.frame();
        }
        if (hp > getMaxHp()) {
            hp = getMaxHp();
            update = true;
        }
        if (shield > getMaxShield()) {
            shield = getMaxShield();
            update = true;
        }
        if (currentWeapon >= getWeaponMax()) {
            currentWeapon = getWeaponMax() - 1;
            update = true;
        }
        return update;
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
