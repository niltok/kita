package ikuyo.api;

import ikuyo.api.cargo.CargoStatic;
import ikuyo.api.equipments.AbstractWeapon;
import ikuyo.api.spaceships.AbstractSpaceship;

public class UserInfo {
    public double x, y = StarInfo.maxTier;

    public double rotation;
    public boolean online;
    public double san = 100;
    public AbstractSpaceship spaceship = new AbstractSpaceship(CargoStatic.shuttle.type());
    public String controlType = "walk";

    public UserInfo() {
        spaceship.weapons.add(new AbstractWeapon(CargoStatic.defaultWeapon.type()));
        spaceship.weapons.add(new AbstractWeapon(CargoStatic.r400.type()));
        spaceship.cargoHold.put(CargoStatic.defaultAmmo.type(), 500);
    }

    public UserInfo(double x, double y) {
        this();
        this.x = x;
        this.y = y;
        online = true;
    }

    public boolean frame() {
        boolean update = false;
        for (var weapon : spaceship.weapons) {
            if (weapon.restFireTime > 0) update = true;
            weapon.frame();
        }
        return update;
    }
}
