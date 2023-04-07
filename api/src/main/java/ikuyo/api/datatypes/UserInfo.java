package ikuyo.api.datatypes;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import ikuyo.api.cargo.CargoStatic;
import ikuyo.api.equipments.Equipment;
import ikuyo.api.equipments.Weapon;
import ikuyo.api.hooks.UserHook;
import ikuyo.api.spaceships.Spaceship;

@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE
)
public class UserInfo {
    public double x, y = StarInfo.maxTier;
    public double cameraX, cameraY = StarInfo.maxTier;

    public double rotation;
    public boolean online;
    @JsonManagedReference
    public UserHook hooks = new UserHook(this);
    public double san = 100;
    @JsonManagedReference
    public Spaceship spaceship;
    public String controlType = "walk";

    public UserInfo() {
        var ship = new Spaceship(CargoStatic.shuttle.type());
        ship.deploy(this);
        new Weapon(CargoStatic.defaultWeapon.type()).equip(spaceship).tryEnable();
        new Weapon(CargoStatic.r400.type()).equip(spaceship).tryEnable();
        new Equipment(CargoStatic.shieldExtender.type()).equip(spaceship).tryEnable();
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
        if (spaceship != null) update |= spaceship.frame();
        return update;
    }
}
