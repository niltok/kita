package ikuyo.api.datatypes;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import ikuyo.api.cargo.CargoStatic;
import ikuyo.api.equipments.Equipment;
import ikuyo.api.equipments.Weapon;
import ikuyo.api.hooks.UserHook;
import ikuyo.api.spaceships.Spaceship;
import ikuyo.utils.DataStatic;
import io.vertx.core.buffer.Buffer;

import java.io.IOException;

@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE
)
public class UserInfo {
    public double x, y = StarInfo.maxTier + 100;
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
        new Weapon(CargoStatic.r400.type()).equip(spaceship).tryEnable();
        new Weapon(CargoStatic.chargeRifle.type()).equip(spaceship).tryEnable();
        new Equipment(CargoStatic.shieldExtender.type()).equip(spaceship).tryEnable();
        spaceship.cargoHold.put(CargoStatic.defaultAmmo.type(), 500);
        spaceship.cargoHold.put(CargoStatic.shieldExtender.type(), 1);
    }

    public UserInfo(double x, double y) {
        this();
        this.x = x;
        this.y = y;
        online = true;
    }

    public boolean frame() {
        if (!online) return false;
        boolean update = false;
        if (spaceship != null) update |= spaceship.frame();
        return update;
    }

    public Buffer toBuffer() {
        try {
            return DataStatic.gzipEncode(DataStatic.mapper.writeValueAsBytes(this));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static UserInfo fromJson(Buffer buffer) {
        try {
            return DataStatic.mapper.readValue(DataStatic.gzipDecode(buffer), UserInfo.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
