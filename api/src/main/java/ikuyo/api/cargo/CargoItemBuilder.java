package ikuyo.api.cargo;

import ikuyo.api.UnpackItem;

public final class CargoItemBuilder {
    private String displayName;
    private String description;
    private double volume;
    private double unpackVolume = Double.POSITIVE_INFINITY;
    private Class<? extends UnpackItem> unpackClass = null;

    private CargoItemBuilder() {
    }

    public static CargoItemBuilder create() {
        return new CargoItemBuilder();
    }

    public CargoItemBuilder withDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public CargoItemBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public CargoItemBuilder withVolume(double volume) {
        this.volume = volume;
        return this;
    }

    public CargoItemBuilder withUnpack(double unpackVolume, Class<? extends UnpackItem> unpackClass) {
        this.unpackVolume = unpackVolume;
        this.unpackClass = unpackClass;
        return this;
    }

    public CargoItem build() {
        return new CargoItem(displayName, description, volume, unpackVolume, unpackClass);
    }
}
