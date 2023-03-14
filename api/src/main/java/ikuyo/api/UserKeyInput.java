package ikuyo.api;

import java.util.Objects;

/** key input fields must be int
 * <p>
 * 0 means not set,
 * 1 means set & auto clear,
 * 2 means set but keep through frames */
public class UserKeyInput {
    public int up, down, left, right, jump;
    public Position position = new Position();

    public boolean input(String fieldName, int value) {
        try {
            var field = this.getClass().getField(fieldName);
            field.set(this, value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    public void frame() {
        try {
            for (var field : this.getClass().getFields()) {
                if (Objects.equals(field.get(this), 1))
                    field.set(this, 0);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
