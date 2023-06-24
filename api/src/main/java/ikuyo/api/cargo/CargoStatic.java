package ikuyo.api.cargo;

import com.google.common.collect.ImmutableList;
import ikuyo.api.datatypes.Damage;
import ikuyo.api.equipments.EquipmentItem;
import ikuyo.api.equipments.EquipmentItemBuilder;
import ikuyo.api.equipments.WeaponItem;
import ikuyo.api.equipments.WeaponItemBuilder;
import ikuyo.api.hooks.ShieldStatic;
import ikuyo.api.spaceships.Spaceship;
import ikuyo.api.spaceships.SpaceshipItem;
import ikuyo.utils.ItemUtils;

@ItemUtils.ItemTarget(CargoItem.class)
public class CargoStatic {
    public static final CargoItem
            soil = new CargoItem("泥土", "岩石碎裂之后的产物", 10),
            stone = new CargoItem("石头", "星球深处的岩浆凝固后的产物", 10);
    public static final SpaceshipItem
            shuttle = new SpaceshipItem("穿梭机", "速度快但是货舱小", 500,
            5000, Spaceship.class, 100, 100, 10,
            2, 5, 5);
    public static final AmmoItem defaultAmmo = new AmmoItem("默认弹药", "", 0.01, 600);
    public static final WeaponItem
            defaultWeapon = WeaponItemBuilder.create("默认武器", "",
                    new Damage(50).setRange(5).setIfBreakBlock(true)).withCollisionRange(0.3).withAmmoType(defaultAmmo).build(),
            r400 = WeaponItemBuilder.create("R400", "",
                    new Damage(1).setRange(0.1)).withFireTime(6)
                    .withAmmoType(defaultAmmo).build(),
            chargeRifle = WeaponItemBuilder.create("ChargeRifle", "",
                    new Damage(50).setRange(0.1).setIfBreakBlock(false)).withFireTime(90)
                    .withAmmoMax(4).withAmmoType(defaultAmmo).build()
    ;

    public static final EquipmentItem shieldExtender = EquipmentItemBuilder.create("护盾扩展器", "")
            .withHooker(ctx -> ctx.hookTokens.add(ctx.hook.put(ShieldStatic.addPercent, 0.5))).build();

    /** 所有货物类型列表 */
    @ItemUtils.ItemList
    private static ImmutableList<CargoItem> itemList;
    public static ImmutableList<CargoItem> itemList() {
        return itemList;
    }
    /** 所有飞船类型列表 */
    public static final ImmutableList<SpaceshipItem> spaceshipList;
    /** 所有装备类型列表 */
    public static final ImmutableList<EquipmentItem> equipmentList;
    /** 所有武器类型列表 */
    public static final ImmutableList<WeaponItem> weaponList;
    /* Auto set CargoItem's type */
    static {
        ItemUtils.setFieldName(CargoStatic.class);
        spaceshipList = ItemUtils.filterType(itemList, SpaceshipItem.class);
        equipmentList = ItemUtils.filterType(itemList, EquipmentItem.class);
        weaponList = ItemUtils.filterType(itemList, WeaponItem.class);
    }
}
