package ikuyo.api.datatypes;

import ikuyo.utils.Position;

import java.time.Instant;

/** key input fields must be int
 * <p>
 * 0 means not set,
 * 1 means set & auto clear,
 * 2 means set but keep through frames */
public class UserInput {
    public int up, down, left, right, jumpOrFly, shot, prevWeapon, nextWeapon;
    public Position relativePointer = new Position(), pointAt = new Position();
    public Instant flyWhen;

    public boolean input(String fieldName, int value) {
        try {
            var field = this.getClass().getField(fieldName);
            field.set(this, value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    public boolean frame() {
        var update = false;
        try {
            for (var field : this.getClass().getFields()) {
                var val = field.get(this);
                if (!(val instanceof Integer i)) continue;
                if (i > 0) update = true;
                if (i == 3) field.set(this, 2); // 上升沿
                if (i == 1) field.set(this, 0); // 下降沿
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return update;
    }
}
