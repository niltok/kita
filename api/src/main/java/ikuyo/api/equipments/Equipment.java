package ikuyo.api.equipments;

import com.fasterxml.jackson.annotation.JsonBackReference;
import ikuyo.api.cargo.UnpackItem;
import ikuyo.api.hooks.HookContext;
import ikuyo.api.hooks.HookToken;
import ikuyo.api.spaceships.Spaceship;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Equipment implements UnpackItem {
    public String type;
    public double hp;
    protected boolean enable;
    public List<HookToken> hookTokens = new ArrayList<>();
    @JsonBackReference
    @Nullable
    public Spaceship spaceship;

    public Equipment() {}
    public Equipment(String type) {
        this.type = type;
        var item = Objects.requireNonNull(EquipmentItem.get(type));
        hp = item.hpMax;
    }

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
        if (spaceship == null) return false;
        if (this.getClass().isAssignableFrom(Equipment.class)) {
            int i = spaceship.passiveEquipments.indexOf(this);
            if (!(i >= 0 && i < spaceship.getPassiveEquipmentMax())) return false;
        }
        return hp > 0;
    }

    public Equipment tryEnable() {
        enable = canEnable();
        if (enable && getInfo().hooker != null) {
            assert spaceship != null;
            getInfo().hooker.accept(new HookContext(hookTokens, spaceship.hooks));
        }
        return this;
    }

    public void disable() {
        if (spaceship != null && spaceship.user != null)
            hookTokens.forEach(token -> spaceship.user.hooks.remove(token));
        enable = false;
    }

    public void unequip() {
        if (spaceship != null) spaceship.passiveEquipments.remove(this);
        disable();
        this.spaceship = null;
    }

    public Equipment equip(Spaceship spaceship) {
        unequip();
        this.spaceship = spaceship;
        if (this.getClass().isAssignableFrom(Equipment.class))
            spaceship.passiveEquipments.add(this);
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
