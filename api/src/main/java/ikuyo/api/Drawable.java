package ikuyo.api;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.vertx.core.json.JsonObject;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
public sealed abstract class Drawable {
    public double x, y;
    /** 单位：弧度 */
    public double angle;
    public int zIndex = 0;
    public boolean interaction = false;
    public static final double scaling = 20.0;
    public static sealed class Sprite extends Drawable {
        public String bundle, asset;
    }
    public static final class Text extends Drawable {
        public String text;
        /** 字体样式 <p>
         * <a href="https://pixijs.download/dev/docs/PIXI.TextStyle.html#defaultStyle">格式参考</a> */
        public JsonObject style = new JsonObject();
    }
    public static final class Container extends Drawable {
        public Drawable[] children;
        public Container() {}
        public Container(Drawable... children) {
            this.children = children;
        }
    }
    public static final class AnimatedSprite extends Sprite {
        public String animation;
        public boolean playing = true;
        public int initialFrame = 0;
    }
}
