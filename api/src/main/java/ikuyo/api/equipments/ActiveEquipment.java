package ikuyo.api.equipments;

import ikuyo.api.cargo.AmmoItem;
import ikuyo.api.hooks.HookContext;
import ikuyo.api.hooks.HookToken;
import ikuyo.api.spaceships.Spaceship;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ActiveEquipment extends Equipment {
    public long restActiveTime = 0;
    public int ammoAmount = 0;
    public boolean reloading = false;
    public List<HookToken> activeHookTokens = new ArrayList<>();

    public ActiveEquipment() {}
    public ActiveEquipment(String type) {
        super(type);
    }

    public long getActiveTime() {
        return getInfo().activeTime;
    }

    public AmmoItem getAmmoType() {
        return getInfo().ammoType;
    }

    public int getAmmoMax() {
        return getInfo().ammoMax;
    }

    public long getReloadingTime() {
        return getInfo().reloadingTime;
    }

    public ActiveEquipmentItem getInfo() {
        return Objects.requireNonNull(ActiveEquipmentItem.get(type));
    }

    /**
     * 装入弹药（只会加装到 {@link Weapon#getAmmoMax()}）
     * @param provide 提供的弹药总量
     * @return 实际使用的弹药量
     * */
    public int reloadAmmo(int provide) {
        var use = Math.min(getAmmoMax() - ammoAmount, provide);
        ammoAmount += use;
        if (use > 0) {
            reloading = true;
            restActiveTime = getReloadingTime();
        }
        return use;
    }

    public boolean tryActive() {
        if (!enable || restActiveTime != 0 || getAmmoType() != null && ammoAmount == 0) return false;
        restActiveTime = getActiveTime();
        if (getAmmoType() != null) ammoAmount--;
        if (getInfo().activeHooker != null) {
            assert spaceship != null;
            getInfo().activeHooker.accept(new HookContext(hookTokens, spaceship.hooks));
        }
        return true;
    }

    public void frame() {
        if (restActiveTime > 0) restActiveTime--;
        if (restActiveTime == 0) {
            reloading = false;
            if (spaceship != null && spaceship.user != null)
                activeHookTokens.forEach(token -> spaceship.user.hooks.remove(token));
        }
    }

    @Override
    public boolean canEnable() {
        if (!super.canEnable()) return false;
        assert spaceship != null;
        if (!this.getClass().isAssignableFrom(ActiveEquipment.class)) return true;
        int i = spaceship.activeEquipments.indexOf(this);
        return i >= 0 && i < spaceship.getActiveEquipmentMax();
    }

    @Override
    public void unequip() {
        if (spaceship != null) spaceship.activeEquipments.remove(this);
        super.unequip();
    }

    @Override
    public ActiveEquipment equip(Spaceship spaceship) {
        super.equip(spaceship);
        if (this.getClass().isAssignableFrom(ActiveEquipment.class))
            spaceship.activeEquipments.add(this);
        return this;
    }

    @Override
    public void pack(Map<String, Integer> items) {
        super.pack(items);
        var ammo = getAmmoType();
        items.put(type, items.getOrDefault(type, 0) + 1);
        items.put(ammo.type(), items.getOrDefault(ammo.type(), 0) + ammoAmount);
    }

    @Override
    public double packSize() {
        return super.packSize() + ammoAmount * getAmmoType().volume;
    }

    @Override
    public String getItemInfo() {
        if (!enable) return "Disabled";
        if (getAmmoType() == null) return "";
        if (reloading) return "%s Reloading".formatted(getAmmoType().displayName);
        return "%s: %d / %d".formatted(getAmmoType().displayName, ammoAmount, getAmmoMax());
    }
}
