package ikuyo.api.spaceships;

import ikuyo.api.CargoHold;
import ikuyo.api.UnpackItem;

import java.util.Map;
import java.util.Objects;

public class AbstractSpaceship implements UnpackItem {
    public String type;
    public double hp, shield;
    public CargoHold cargoHold;
    public AbstractSpaceship(String type) {
        this.type = type;
        var ship = Objects.requireNonNull(SpaceshipItem.get(type));
        hp = getMaxHp();
        shield = getMaxShield();
        cargoHold = new CargoHold(ship.cargoVolume);
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
        return hp >= getMaxHp() && shield >= getMaxShield();
    }

    @Override
    public void pack(Map<String, Integer> items) {
        var n = items.get(type);
        if (n == null) n = 0;
        items.put(type, n + 1);
    }

    @Override
    public double packSize() {
        return Objects.requireNonNull(SpaceshipItem.get(type)).volume;
    }

    @Override
    public String getItemInfo() {
        return "";
    }
}
