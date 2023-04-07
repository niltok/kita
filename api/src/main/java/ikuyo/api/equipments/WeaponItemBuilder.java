package ikuyo.api.equipments;

import ikuyo.api.cargo.AmmoItem;
import ikuyo.api.cargo.UnpackItem;
import ikuyo.api.datatypes.Damage;
import ikuyo.api.hooks.HookContext;

import java.util.function.Consumer;

public class WeaponItemBuilder extends ActiveEquipmentItemBuilder {
    protected Damage damage;
    protected double velocity = 150;
    protected double collisionRange = 0.1;
    protected long fireTime = 60;

    private WeaponItemBuilder() {
        super();
        unpackClass = Weapon.class;
    }

    public static WeaponItemBuilder create() {
        return new WeaponItemBuilder();
    }

    public static WeaponItemBuilder create(String displayName, String description, Damage damage) {
        return new WeaponItemBuilder().withDisplayName(displayName).withDescription(description).withDamage(damage);
    }

    public WeaponItemBuilder withDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public WeaponItemBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public WeaponItemBuilder withVolume(double volume) {
        this.volume = volume;
        return this;
    }

    public WeaponItemBuilder withUnpackVolume(double unpackVolume) {
        this.unpackVolume = unpackVolume;
        return this;
    }

    public WeaponItemBuilder withUnpackClass(Class<? extends UnpackItem> unpackClass) {
        this.unpackClass = unpackClass;
        return this;
    }

    public WeaponItemBuilder withHpMax(double hpMax) {
        this.hpMax = hpMax;
        return this;
    }

    public WeaponItemBuilder withDamage(Damage damage) {
        this.damage = damage;
        return this;
    }

    public WeaponItemBuilder withAmmoMax(int ammoMax) {
        this.ammoMax = ammoMax;
        return this;
    }

    public WeaponItemBuilder withVelocity(double velocity) {
        this.velocity = velocity;
        return this;
    }

    public WeaponItemBuilder withCollisionRange(double collisionRange) {
        this.collisionRange = collisionRange;
        return this;
    }

    public WeaponItemBuilder withFireTime(long fireTime) {
        this.fireTime = fireTime;
        return this;
    }

    public WeaponItemBuilder withReloadingTime(long reloadingTime) {
        this.reloadingTime = reloadingTime;
        return this;
    }

    public WeaponItemBuilder withAmmoType(AmmoItem ammoType) {
        this.ammoType = ammoType;
        return this;
    }

    public WeaponItemBuilder withHooker(Consumer<HookContext> hooker) {
        this.hooker = hooker;
        return this;
    }

    public WeaponItemBuilder withActiveHooker(Consumer<HookContext> activeHooker) {
        this.activeHooker = activeHooker;
        return this;
    }

    public WeaponItem build() {
        return new WeaponItem(displayName,
                description,
                volume,
                unpackVolume,
                unpackClass,
                hpMax,
                hooker,
                damage,
                ammoMax,
                velocity,
                collisionRange,
                fireTime,
                reloadingTime,
                ammoType,
                activeHooker);
    }
}
