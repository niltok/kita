package ikuyo.api.equipments;

import ikuyo.api.cargo.CargoItemBuilder;
import ikuyo.api.cargo.UnpackItem;
import ikuyo.api.hooks.HookContext;

import java.util.function.Consumer;

public class EquipmentItemBuilder extends CargoItemBuilder {
    protected double hpMax;
    protected Consumer<HookContext> hooker;

    EquipmentItemBuilder() {
        super();
        volume = 5;
        unpackVolume = 5;
        hpMax = 10;
        unpackClass = Equipment.class;
    }

    public static EquipmentItemBuilder create() {
        return new EquipmentItemBuilder();
    }

    public static EquipmentItemBuilder create(String displayName, String description) {
        return new EquipmentItemBuilder().withDisplayName(displayName).withDescription(description);
    }

    public EquipmentItemBuilder withDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public EquipmentItemBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public EquipmentItemBuilder withVolume(double volume) {
        this.volume = volume;
        return this;
    }

    public EquipmentItemBuilder withUnpackVolume(double unpackVolume) {
        this.unpackVolume = unpackVolume;
        return this;
    }

    public EquipmentItemBuilder withUnpackClass(Class<? extends UnpackItem> unpackClass) {
        this.unpackClass = unpackClass;
        return this;
    }

    public EquipmentItemBuilder withHpMax(double hpMax) {
        this.hpMax = hpMax;
        return this;
    }

    public EquipmentItemBuilder withHooker(Consumer<HookContext> hooker) {
        this.hooker = hooker;
        return this;
    }

    public EquipmentItem build() {
        return new EquipmentItem(displayName, description, volume, unpackVolume, unpackClass, hpMax, hooker);
    }
}
