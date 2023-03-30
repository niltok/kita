package ikuyo.api;

import com.google.common.collect.ImmutableList;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Objects;

/** 货物类型 */
public class CargoItem {
    private String type;
    public final String displayName, description;
    public final double volume, unpackVolume;
    public final Class<? extends UnpackItem> unpackClass;
    public CargoItem(
            String displayName,
            String description,
            double volume,
            double unpackVolume,
            Class<? extends UnpackItem> unpackClass) {
        this.displayName = displayName;
        this.description = description;
        this.volume = volume;
        this.unpackVolume = unpackVolume;
        this.unpackClass = unpackClass;
    }
    /** 构造无法 unpack 的 Item */
    public CargoItem(String displayName, String description, int volume) {
        this(displayName, description, volume, Double.POSITIVE_INFINITY, null);
    }
    public String type() {
        return type;
    }
    private static <T> Constructor<T> getConstructor(Class<T> unpack, Class<?>... params) {
        try {
            return unpack.getConstructor(params);
        } catch (Exception ignore) {
            return null;
        }
    }
    public UnpackItem unpack() {
        if (unpackClass == null) return null;
        try {
            var cons = getConstructor(unpackClass, CargoItem.class);
            if (cons != null) return cons.newInstance(this);
            cons = getConstructor(unpackClass, String.class);
            if (cons != null) return cons.newInstance(type);
            cons = getConstructor(unpackClass);
            return Objects.requireNonNull(cons).newInstance();
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    public static CargoItem get(String index) {
        try {
            return (CargoItem) CargoItem.class.getField(index).get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            return null;
        }
    }
    /** 所有货物类型列表 */
    public static final ImmutableList<CargoItem> itemList;
    /* Auto set CargoItem's name */
    static {
        var temp = new ArrayList<CargoItem>();
        for (var field : CargoStatic.class.getFields()) {
            try {
                if (Modifier.isStatic(field.getModifiers())) {
                    var obj = field.get(null);
                    if (obj instanceof CargoItem item) {
                        temp.add(item);
                        item.type = field.getName();
                    }
                }
            } catch (Exception ignore) {}
        }
        itemList = ImmutableList.copyOf(temp);
    }
}
