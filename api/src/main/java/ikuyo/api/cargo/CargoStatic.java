package ikuyo.api.cargo;

import com.google.common.collect.ImmutableList;
import ikuyo.api.equipments.AbstractWeapon;
import ikuyo.api.equipments.EquipmentItem;
import ikuyo.api.equipments.WeaponItem;
import ikuyo.api.spaceships.AbstractSpaceship;
import ikuyo.api.spaceships.SpaceshipItem;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CargoStatic {
    public static final CargoItem
            soil = new CargoItem("泥土", "岩石碎裂之后的产物", 10),
            stone = new CargoItem("石头", "星球深处的岩浆凝固后的产物", 10);
    public static final SpaceshipItem
            shuttle = new SpaceshipItem("穿梭机", "速度快但是货舱小", 500,
            5000, AbstractSpaceship.class, 100, 100, 10, 1);
    public static final AmmoItem defaultAmmo = new AmmoItem("默认弹药", "", 0.01);
    public static final WeaponItem
            defaultWeapon = new WeaponItem("默认武器", "",
            5, 5, AbstractWeapon.class, 10, 50,
            60, 60, defaultAmmo);

    public static <W, T> ImmutableList<T> filterType(List<W> list, Class<T> clazz) {
        return ImmutableList.copyOf(list.stream()
                .map(i -> clazz.isInstance(i) ? (T)i : null)
                .filter(Objects::nonNull).toList());
    }
    /** 所有货物类型列表 */
    public static final ImmutableList<CargoItem> itemList;
    /** 所有飞船类型列表 */
    public static final ImmutableList<SpaceshipItem> spaceshipList;
    /** 所有装备类型列表 */
    public static final ImmutableList<EquipmentItem> equipmentList;
    /** 所有武器类型列表 */
    public static final ImmutableList<WeaponItem> weaponList;
    /* Auto set CargoItem's type */
    static {
        var temp = new ArrayList<CargoItem>();
        for (var field : CargoStatic.class.getFields()) {
            try {
                if (Modifier.isStatic(field.getModifiers())) {
                    var obj = field.get(null);
                    if (obj instanceof CargoItem item) {
                        temp.add(item);
                        var type = CargoItem.class.getDeclaredField("type");
                        type.setAccessible(true);
                        type.set(item, field.getName());
                    }
                }
            } catch (Exception ignore) {}
        }
        itemList = ImmutableList.copyOf(temp);
        spaceshipList = filterType(temp, SpaceshipItem.class);
        equipmentList = filterType(temp, EquipmentItem.class);
        weaponList = filterType(temp, WeaponItem.class);
    }
}
