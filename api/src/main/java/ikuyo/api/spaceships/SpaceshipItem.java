package ikuyo.api.spaceships;

import com.google.common.collect.ImmutableList;
import ikuyo.api.CargoItem;
import ikuyo.api.CargoStatic;
import ikuyo.api.UnpackItem;

import java.lang.reflect.Modifier;
import java.util.ArrayList;

public class SpaceshipItem extends CargoItem {
    public double hpMax, shieldMax;
    public SpaceshipItem(String displayName, String description, double volume, double unpackVolume,
                         Class<? extends UnpackItem> unpackClass, double hpMax, double shieldMax) {
        super(displayName, description, volume, unpackVolume, unpackClass);
        this.hpMax = hpMax;
        this.shieldMax = shieldMax;
    }
    public static SpaceshipItem get(String index) {
        try {
            return (SpaceshipItem) CargoItem.class.getField(index).get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            return null;
        }
    }
    /** 所有飞船类型列表 */
    public static final ImmutableList<SpaceshipItem> itemList;
    /* Auto collect itemList */
    static {
        var temp = new ArrayList<SpaceshipItem>();
        for (var field : CargoStatic.class.getFields()) {
            try {
                if (Modifier.isStatic(field.getModifiers())) {
                    var obj = field.get(null);
                    if (obj instanceof SpaceshipItem item) {
                        temp.add(item);
                    }
                }
            } catch (Exception ignore) {}
        }
        itemList = ImmutableList.copyOf(temp);
    }
}
