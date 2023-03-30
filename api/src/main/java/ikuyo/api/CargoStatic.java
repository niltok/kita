package ikuyo.api;

import ikuyo.api.spaceships.AbstractSpaceship;
import ikuyo.api.spaceships.SpaceshipItem;

public class CargoStatic {
    public static final CargoItem
            soil = new CargoItem("泥土", "岩石碎裂之后的产物", 10),
            stone = new CargoItem("石头", "星球深处的岩浆凝固后的产物", 10);
    public static final SpaceshipItem
            shuttle = new SpaceshipItem("穿梭机", "速度快但是货舱小", 500,
            5000, AbstractSpaceship.class, 100, 100, 10);
}
