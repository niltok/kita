package ikuyo.api.spaceships;

import ikuyo.api.UnpackItem;

import java.util.Map;
import java.util.Objects;

public class AbstractSpaceship implements UnpackItem {
    public String type;
    public double hp, hpMax, shield, shieldMax;
    public long cargoHold;
    public AbstractSpaceship(String type) {
        this.type = type;
        var item = Objects.requireNonNull(SpaceshipItem.get(type));
        hpMax = item.hpMax;
        shieldMax = item.shieldMax;
        hp = hpMax;
        shield = shieldMax;
    }
    @Override
    public String getItemType() {
        return type;
    }

    @Override
    public boolean canPack() {
        return hp == hpMax && shield == shieldMax;
    }

    @Override
    public void pack(Map<String, Integer> items) {
        var n = items.get(type);
        if (n == null) n = 0;
        items.put(type, n + 1);
    }

    @Override
    public String getItemInfo() {
        return "";
    }
}
