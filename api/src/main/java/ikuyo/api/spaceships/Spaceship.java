package ikuyo.api.spaceships;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import ikuyo.api.cargo.CargoHold;
import ikuyo.api.cargo.CargoItem;
import ikuyo.api.cargo.UnpackItem;
import ikuyo.api.datatypes.UIElement;
import ikuyo.api.datatypes.UserInfo;
import ikuyo.api.equipments.ActiveEquipment;
import ikuyo.api.equipments.Equipment;
import ikuyo.api.equipments.EquipmentItem;
import ikuyo.api.equipments.Weapon;
import ikuyo.api.hooks.SpaceshipHook;
import io.vertx.core.json.JsonObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Spaceship implements UnpackItem {
    public String type, name;
    public double hp, shield;
    public CargoHold cargoHold;
    @JsonManagedReference
    public List<Weapon> weapons;
    @JsonManagedReference
    public List<Equipment> passiveEquipments;
    @JsonManagedReference
    public List<ActiveEquipment> activeEquipments;
    public int currentWeapon = 0;
    @JsonBackReference
    @Nullable
    public UserInfo user;
    @JsonManagedReference
    public SpaceshipHook hooks = new SpaceshipHook(this);

    public Spaceship(String type) {
        this.type = type;
        var ship = Objects.requireNonNull(SpaceshipItem.get(type));
        this.name = ship.displayName;
        hp = getMaxHp();
        shield = getMaxShield();
        cargoHold = new CargoHold(ship.cargoVolume);
        weapons = new ArrayList<>(getWeaponMax());
        passiveEquipments = new ArrayList<>(getPassiveEquipmentMax());
        activeEquipments = new ArrayList<>(getActiveEquipmentMax());
    }

    public Spaceship() {}

    public Weapon getCurrentWeapon() {
        return weapons.get(currentWeapon);
    }

    public double getMaxHp() {
        return getInfo().hpMax;
    }

    public int getWeaponMax() {
        return getInfo().weaponMax;
    }

    public int getActiveEquipmentMax() {
        return getInfo().activeEquipmentMax;
    }

    public int getPassiveEquipmentMax() {
        return getInfo().passiveEquipmentMax;
    }

    public double getMaxShield() {
        return hooks.getShield();
    }

    public SpaceshipItem getInfo() {
        return Objects.requireNonNull(SpaceshipItem.get(type));
    }

    public void undeploy() {
        if (user != null) user.spaceship = null;
        user = null;
    }

    public void deploy(UserInfo user) {
        undeploy();
        if (user.spaceship != null) user.spaceship.undeploy();
        user.spaceship = this;
        this.user = user;
    }

    public void inflict(double shieldDamage, double hpDamage) {
        this.shield = Math.max(this.shield - shieldDamage, 0);
        this.hp = Math.max(this.hp - hpDamage, 0);
    }

    public boolean tryFire() {
        var weapon = getCurrentWeapon();
        var ammo = weapon.getAmmoType();
        var flag = weapon.tryActive();
        if (!flag && ammo != null && weapon.ammoAmount == 0)
            cargoHold.take(ammo.type(), weapon.reloadAmmo(cargoHold.items.getOrDefault(ammo.type(), 0)));
        return flag;
    }

    public boolean frame() {
        boolean update = false;
        for (var weapon : weapons) {
            if (weapon.restActiveTime > 0) update = true;
            weapon.frame();
        }
        if (hp > getMaxHp()) {
            hp = getMaxHp();
            update = true;
        }
        if (shield > getMaxShield()) {
            shield = getMaxShield();
            update = true;
        }
        if (currentWeapon >= getWeaponMax()) {
            currentWeapon = getWeaponMax() - 1;
            update = true;
        }
        return update;
    }

    @Override
    public String getItemType() {
        return type;
    }

    @Override
    public boolean canPack() {
        return hp >= getMaxHp() && shield >= getMaxShield()
                && weapons.stream().allMatch(w -> w == null || w.canPack());
    }

    @Override
    public void pack(Map<String, Integer> items) {
        items.put(type, items.getOrDefault(type, 0) + 1);
        for (var w : weapons) w.pack(items);
    }

    @Override
    public double packSize() {
        return Objects.requireNonNull(SpaceshipItem.get(type)).volume
                + weapons.stream().mapToDouble(w -> w == null ? 0 : w.packSize()).sum();
    }

    @Override
    public String getItemInfo() {
        return "";
    }

    public UIElement renderEditor(Map<String, CargoHold> cargos) {
        var cargo = new ArrayList<UIElement>();
        var equip = new ArrayList<UIElement>();
        cargo.add(UIElement.titleLabel("舰船货舱", "剩余容量: %.2f".formatted(cargoHold.getRestVolume())));
        renderCargoHold(cargoHold, "ship", cargo);
        for (var e : cargos.entrySet()) {
            cargo.add(UIElement.titleLabel(e.getKey()));
            renderCargoHold(e.getValue(), e.getKey(), cargo);
        }
        equip.add(UIElement.titleLabel("武器"));
        renderEquip(weapons, "weapon", equip);
        equip.add(UIElement.titleLabel("主动装备"));
        renderEquip(activeEquipments, "active", equip);
        equip.add(UIElement.titleLabel("被动装备"));
        renderEquip(passiveEquipments, "passive", equip);
        return UIElement.div(
                UIElement.div(
                        new UIElement.Stateful("input.text", "ship-name", name)
                                .appendClass("auto-expand"),
                        UIElement.callbackText("修改",
                                JsonObject.of("type", "ship.name"), "ship-name")
                ).appendClass("label-item"),
                UIElement.div(
                        UIElement.div(cargo.toArray(UIElement[]::new)),
                        UIElement.div(equip.toArray(UIElement[]::new))
                ).appendClass("multi-column")
        );
    }

    private <T extends Equipment> void renderEquip(List<T> equip, String src, List<UIElement> uis) {
        for (int i = 0, equipSize = equip.size(); i < equipSize; i++) {
            T e = equip.get(i);
            var item = Objects.requireNonNull(CargoItem.get(e.getItemType()));
            JsonObject callback = JsonObject.of(
                    "type", "ship.unequip",
                    "src", src,
                    "index", i);
            uis.add(UIElement.labelItem(
                    new UIElement.Text(item.displayName),
                    new UIElement.Text(e.getItemInfo()),
                    callback
            ).appendClass("hover-label").withTitle(item.description));
        }
    }

    private void renderCargoHold(CargoHold cargo, String src, List<UIElement> uis) {
        List<UnpackItem> unpacks = cargo.unpacks;
        for (int i = 0, unpacksSize = unpacks.size(); i < unpacksSize; i++) {
            UnpackItem unpack = unpacks.get(i);
            if (!(unpack instanceof Equipment)) continue;
            var item = Objects.requireNonNull(CargoItem.get(unpack.getItemType()));
            JsonObject callback = JsonObject.of(
                    "type", "ship.equip",
                    "src", src,
                    "index", i);
            uis.add(UIElement.labelItem(
                    new UIElement.Text(item.displayName),
                    new UIElement.Text(unpack.getItemInfo()),
                    callback
            ).appendClass("hover-label").withTitle(item.description));
        }
        cargo.items.forEach((type, num) -> {
            var item = Objects.requireNonNull(CargoItem.get(type));
            if (item.unpackClass == null || !(Equipment.class.isAssignableFrom(item.unpackClass))) return;
            JsonObject callback = JsonObject.of(
                    "type", "ship.equip",
                    "src", src,
                    "key", type);
            uis.add(UIElement.labelItem(
                    new UIElement.Text(item.displayName),
                    new UIElement.Text("Num: %d".formatted(num)),
                    callback
            ).appendClass("hover-label").withTitle(item.description));
        });
    }

    public void handleUserEvents(Map<String, List<JsonObject>> events, Map<String, CargoHold> cargos) {
        var equipMsg = events.get("ship.equip");
        if (equipMsg != null) for (var msg : equipMsg) {
            var src = msg.getString("src");
            var cargo = switch (src) {
                case "ship" -> cargoHold;
                case null -> null;
                default -> cargos.get(src);
            };
            if (cargo == null) continue;
            if (msg.containsKey("key")) {
                var key = msg.getString("key");
                var item = EquipmentItem.get(key);
                if (item == null) continue;
                var e = item.unpack();
                if (!e.equip(this).tryEnable().isEnable() || cargo.take(key) != 0) {
                    e.unequip();
                }
            } else {
                var index = msg.getInteger("index");
                var unpack = cargo.unpacks.get(index);
                if (!(unpack instanceof Equipment e)) continue;
                if (!e.equip(this).tryEnable().isEnable() || cargo.take(index) != 0) {
                    e.unequip();
                }
            }
        }
        var unequipMsg = events.get("ship.unequip");
        if (unequipMsg != null) for (var msg : unequipMsg) {
            var index = msg.getInteger("index");
            if (index == null) continue;
            var e = switch (msg.getString("src")) {
                case "weapon" -> weapons.get(index);
                case "active" -> activeEquipments.get(index);
                case "passive" -> passiveEquipments.get(index);
                case null, default -> null;
            };
            if (e == null) continue;
            if (cargoHold.put(e) == 0) {
                e.unequip();
            }
        }
        var nameMsg = events.get("ship.name");
        if (nameMsg != null && !nameMsg.isEmpty()) {
            var msg = nameMsg.get(nameMsg.size() - 1);
            var s = msg.getJsonObject("states").getString("ship-name");
            if (!s.isEmpty()) name = s;
        }
    }
}
