package ikuyo.api;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
public sealed abstract class Drawable {
    public double x, y, angle;
    public static final class Sprite extends Drawable {
        public String type;
    }
}
