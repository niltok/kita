package ikuyo.api.equipments;

import ikuyo.api.datatypes.Damage;
import ikuyo.api.spaceships.Spaceship;

import java.util.Objects;

public class Weapon extends ActiveEquipment {
    public Weapon(String type) {
        super(type);
    }

    public Weapon() {}

    public Damage getDamage() {
        return getInfo().damage;
    }

    public WeaponItem getInfo() {
        return Objects.requireNonNull(WeaponItem.get(type));
    }

    @Override
    public boolean canEnable() {
        if (!super.canEnable()) return false;
        assert spaceship != null;
        if (!this.getClass().isAssignableFrom(Weapon.class)) return true;
        int i = spaceship.weapons.indexOf(this);
        return i >= 0 && i < spaceship.getWeaponMax();
    }

    @Override
    public void unequip() {
        if (spaceship != null) spaceship.weapons.remove(this);
        super.unequip();
    }

    @Override
    public Weapon equip(Spaceship spaceship) {
        super.equip(spaceship);
        if (this.getClass().isAssignableFrom(Weapon.class))
            spaceship.weapons.add(this);
        return this;
    }
}
