package ikuyo.api;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import ikuyo.utils.RawDeserializer;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Drawable.Sprite.class, name = "Sprite"),
        @JsonSubTypes.Type(value = Drawable.Text.class, name = "Text"),
        @JsonSubTypes.Type(value = Drawable.Container.class, name = "Container"),
        @JsonSubTypes.Type(value = Drawable.AnimatedSprite.class, name = "AnimatedSprite")
})
public sealed abstract class Drawable {
    public double x, y;
    /** 单位：弧度 */
    public double rotation;
    public int zIndex = 0;
    public boolean interaction = false;
    public static final double scaling = 20.0;

    public static sealed class Sprite extends Drawable {
        public String bundle, asset;
    }
    public static final class Text extends Drawable {
        public String text;
        /** 字体样式 Json 格式 <p>
         * <a href="https://pixijs.download/dev/docs/PIXI.TextStyle.html#defaultStyle">格式参考</a> */
        @JsonRawValue
        @JsonDeserialize(using = RawDeserializer.class)
        public String style = "{}";
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
