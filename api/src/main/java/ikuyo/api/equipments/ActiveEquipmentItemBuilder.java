package ikuyo.api.equipments;

import ikuyo.api.cargo.AmmoItem;
import ikuyo.api.cargo.UnpackItem;
import ikuyo.api.hooks.HookContext;

import java.util.function.Consumer;

public class ActiveEquipmentItemBuilder extends EquipmentItemBuilder {
    protected long activeTime = 60;
    protected int ammoMax = 60;
    protected long reloadingTime = 90;
    protected AmmoItem ammoType = null;
    protected Consumer<HookContext> activeHooker;

    ActiveEquipmentItemBuilder() {
        super();
        unpackClass = ActiveEquipment.class;
    }

    public static ActiveEquipmentItemBuilder create() {
        return new ActiveEquipmentItemBuilder();
    }

    public static ActiveEquipmentItemBuilder create(String displayName, String description) {
        return new ActiveEquipmentItemBuilder().withDisplayName(displayName).withDescription(description);
    }

    public ActiveEquipmentItemBuilder withDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public ActiveEquipmentItemBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public ActiveEquipmentItemBuilder withVolume(double volume) {
        this.volume = volume;
        return this;
    }

    public ActiveEquipmentItemBuilder withUnpackVolume(double unpackVolume) {
        this.unpackVolume = unpackVolume;
        return this;
    }

    public ActiveEquipmentItemBuilder withUnpackClass(Class<? extends UnpackItem> unpackClass) {
        this.unpackClass = unpackClass;
        return this;
    }

    public ActiveEquipmentItemBuilder withActiveTime(long activeTime) {
        this.activeTime = activeTime;
        return this;
    }

    public ActiveEquipmentItemBuilder withAmmoMax(int ammoMax) {
        this.ammoMax = ammoMax;
        return this;
    }

    public ActiveEquipmentItemBuilder withReloadingTime(long reloadingTime) {
        this.reloadingTime = reloadingTime;
        return this;
    }

    public ActiveEquipmentItemBuilder withAmmoType(AmmoItem ammoType) {
        this.ammoType = ammoType;
        return this;
    }

    public ActiveEquipmentItemBuilder withHpMax(double hpMax) {
        this.hpMax = hpMax;
        return this;
    }

    public ActiveEquipmentItemBuilder withHooker(Consumer<HookContext> hooker) {
        this.hooker = hooker;
        return this;
    }

    public ActiveEquipmentItemBuilder withActiveHooker(Consumer<HookContext> activeHooker) {
        this.activeHooker = activeHooker;
        return this;
    }

    public ActiveEquipmentItem build() {
        return new ActiveEquipmentItem(displayName,
                description,
                volume,
                unpackVolume,
                unpackClass,
                hpMax,
                hooker,
                activeTime,
                ammoMax,
                reloadingTime,
                ammoType,
                activeHooker);
    }
}
