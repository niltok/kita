package ikuyo.api.equipments;

import com.google.common.collect.ImmutableList;
import ikuyo.api.UnpackItem;
import ikuyo.api.cargo.AmmoItem;
import ikuyo.api.cargo.CargoStatic;

import java.lang.reflect.Modifier;
import java.util.ArrayList;

public class WeaponItem extends EquipmentItem {
    public final double damage;
    public final int ammoMax;
    public final long fireTime;
    public final AmmoItem ammoType;

    public WeaponItem(String displayName,
                      String description,
                      double volume,
                      double unpackVolume,
                      Class<? extends UnpackItem> unpackClass,
                      double hpMax,
                      double damage,
                      int ammoMax,
                      long fireTime,
                      AmmoItem ammoType) {
        super(displayName, description, volume, unpackVolume, unpackClass, hpMax);
        this.damage = damage;
        this.ammoMax = ammoMax;
        this.fireTime = fireTime;
        this.ammoType = ammoType;
    }
    public static WeaponItem get(String index) {
        try {
            return (WeaponItem) CargoStatic.class.getField(index).get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            return null;
        }
    }
    /** 所有武器类型列表 */
    public static final ImmutableList<WeaponItem> itemList;
    /* Auto collect itemList */
    static {
        var temp = new ArrayList<WeaponItem>();
        for (var field : CargoStatic.class.getFields()) {
            try {
                if (Modifier.isStatic(field.getModifiers())) {
                    var obj = field.get(null);
                    if (obj instanceof WeaponItem item) {
                        temp.add(item);
                    }
                }
            } catch (Exception ignore) {}
        }
        itemList = ImmutableList.copyOf(temp);
    }
}
