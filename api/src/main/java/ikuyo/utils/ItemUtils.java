package ikuyo.utils;

import com.google.common.collect.ImmutableList;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ItemUtils {
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ItemIgnore {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ItemList {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.TYPE})
    public @interface ItemInject {
        Class<?>[] value();
    }

    public static <T> List<T> setFieldName(String prefix, Class<?> entry, Class<T> target, String nameField) {
        var res = new ArrayList<T>();
        Field itemList = null;
        for (var field : entry.getDeclaredFields()) {
            try {
                if (!Modifier.isStatic(field.getModifiers())
                        || field.isAnnotationPresent(ItemIgnore.class)) continue;
                if (field.isAnnotationPresent(ItemList.class)) {
                    itemList = field;
                    continue;
                }
                if (field.isAnnotationPresent(ItemInject.class)) {
                    res.addAll(setFieldName(prefix + field.getName() + ".",
                            field.getType(), target, nameField));
                    continue;
                }
                var obj = field.get(null);
                if (target.isInstance(obj)) {
                    res.add((T) obj);
                    var type = target.getDeclaredField(nameField);
                    type.setAccessible(true);
                    type.set(obj, prefix + field.getName());
                }
            } catch (Exception ignore) {}
        }
        var inject = entry.getAnnotation(ItemInject.class);
        if (inject != null) for (var next : inject.value()) {
            res.addAll(setFieldName(prefix + next.getSimpleName() + ".",
                    next, target, nameField));
        }
        try {
            if (itemList == null) return res;
            itemList.setAccessible(true);
            var type = itemList.getType();
            if (type.isAssignableFrom(ArrayList.class))
                itemList.set(null, res);
            if (type.isAssignableFrom(ImmutableList.class))
                itemList.set(null, ImmutableList.copyOf(res));
        } catch (Exception ignore) {}
        return res;
    }

    public static <T> List<T> setFieldName(Class<?> entry, Class<T> target, String nameField) {
        return setFieldName("", entry, target, nameField);
    }

    public static <W, T> ImmutableList<T> filterType(List<W> list, Class<T> clazz) {
        return ImmutableList.copyOf(list.stream()
                .map(i -> clazz.isInstance(i) ? (T)i : null)
                .filter(Objects::nonNull).toList());
    }
}
