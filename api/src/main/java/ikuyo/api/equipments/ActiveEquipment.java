package ikuyo.api.equipments;

import java.util.Objects;

public class ActiveEquipment extends Equipment {
    public long restActiveTime = 0;

    public long getActiveTime() {
        return getInfo().activeTime;
    }

    public ActiveEquipmentItem getInfo() {
        return Objects.requireNonNull(ActiveEquipmentItem.get(type));
    }

    public boolean tryActive() {
        if (restActiveTime != 0) return false;
        restActiveTime = getActiveTime();
        return true;
    }

    public void frame() {
        if (restActiveTime > 0) restActiveTime--;
    }
}
