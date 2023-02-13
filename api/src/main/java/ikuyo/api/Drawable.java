package ikuyo.api;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;

public sealed abstract class Drawable {
    public double x, y;
    /** 单位：弧度 */
    public double angle;
    public int zIndex = 0;
    public boolean interaction = false;
    public static final double scaling = 20.0;
    public JsonObject toJson() {
        return JsonObject.of(
                "x", x,
                "y", y,
                "angle", angle,
                "zIndex", zIndex,
                "interaction", interaction);
    }
    public static sealed class Sprite extends Drawable {
        public String bundle, asset;

        @Override
        public JsonObject toJson() {
            return super.toJson().put("@type", "Sprite").put("bundle", bundle).put("asset", asset);
        }
    }
    public static final class Text extends Drawable {
        public String text;
        /** 字体样式 <p>
         * <a href="https://pixijs.download/dev/docs/PIXI.TextStyle.html#defaultStyle">格式参考</a> */
        public JsonObject style = new JsonObject();

        @Override
        public JsonObject toJson() {
            return super.toJson().put("@type", "Text").put("text", text).put("style", style);
        }
    }
    public static final class Container extends Drawable {
        public Drawable[] children;
        public Container() {}
        public Container(Drawable... children) {
            this.children = children;
        }

        @Override
        public JsonObject toJson() {
            return super.toJson().put("@type", "Container").put("children",
                    new JsonArray(Arrays.stream(children).map(Drawable::toJson).toList()));
        }
    }
    public static final class AnimatedSprite extends Sprite {
        public String animation;
        public boolean playing = true;
        public int initialFrame = 0;

        @Override
        public JsonObject toJson() {
            return super.toJson().put("@type", "AnimatedSprite").put("animation", animation)
                    .put("playing", playing).put("initialFrame", initialFrame);
        }
    }
}
