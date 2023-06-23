package ikuyo.api.datatypes;

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
        @JsonSubTypes.Type(value = Drawable.AnimatedSprite.class, name = "AnimatedSprite"),
        @JsonSubTypes.Type(value = Drawable.Line.class, name = "Line")
})
public sealed abstract class Drawable implements Cloneable{
    public double x, y;
    /** 单位：弧度 */
    public double rotation;
    public int zIndex = 0, user = -1;
    public static final double scaling = 20.0;

    @Override
    public Drawable clone() {
        try {
            return (Drawable) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

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
    public static final class Line extends Drawable {
        public double length, width;
        public int color;

        public Line setStartPoint(double x, double y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public Line setEndPoint(double x, double y) {
            this.lineTo(x, y);
            return this;
        }

        public Line setWidth(double width) {
            this.width = width;
            return this;
        }

        public Line setColor(int color) {
            this.color = color;
            return this;
        }

        public void lineTo(double x, double y) {
            length = Math.hypot(x - this.x, y - this.y);
            rotation = -Math.atan2(x - this.x, y - this.y) + Math.PI / 2;
        }
    }
}
