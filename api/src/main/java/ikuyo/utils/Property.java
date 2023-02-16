package ikuyo.utils;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class Property<T> implements Supplier<T>, Consumer<T> {
    T value;

    public Property(T v) {
        value = v;
    }

    public Property() {}

    @Override
    public void accept(T v) {
        value = v;
    }

    public void set(T v) {
        value = v;
    }

    @Override
    public T get() {
        return value;
    }
}
