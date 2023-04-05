package ikuyo.api.equipments;

import com.fasterxml.jackson.annotation.JsonBackReference;
import ikuyo.api.cargo.UnpackItem;
import ikuyo.api.spaceships.Spaceship;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;

public abstract class Equipment implements UnpackItem {
    public String type;
    public double hp;
    protected boolean enable;
    @JsonBackReference
    @Nullable
    public Spaceship spaceship;

    public double getMaxHp() {
        return getInfo().hpMax;
    }

    public EquipmentItem getInfo() {
        return Objects.requireNonNull(EquipmentItem.get(type));
    }

    public boolean isEnable() {
        if (spaceship == null) return false;
        return enable;
    }

    public boolean canEnable() {
        return spaceship != null && hp > 0;
    }

    public Equipment tryEnable() {
        enable = canEnable();
        return this;
    }

    public void disable() {
        enable = false;
    }

    public void unequip() {
        disable();
        this.spaceship = null;
    }

    public Equipment equip(Spaceship spaceship) {
        unequip();
        this.spaceship = spaceship;
        enable = false;
        return this;
    }

    @Override
    public String getItemType() {
        return type;
    }

    @Override
    public boolean canPack() {
        return hp == getMaxHp();
    }

    @Override
    public double packSize() {
        return getInfo().volume;
    }

    @Override
    public void pack(Map<String, Integer> items) {
        unequip();
    }

    @Override
    public String getItemInfo() {
        if (!enable) return "Disabled";
        return "HP: %.1f / %.1f".formatted(hp, getMaxHp());
    }
}
