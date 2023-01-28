package ikuyo.api;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.vertx.core.json.JsonObject;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
public sealed abstract class Drawable {
    public double x, y;
    /** 单位：弧度 */
    public double angle;
    public static final class Sprite extends Drawable {
        public String bundle, asset;
    }
    public static final class Text extends Drawable {
        public String text;
        /** 字体样式 <p>
         * <a href="https://pixijs.download/dev/docs/PIXI.TextStyle.html#defaultStyle">格式参考</a> */
        public JsonObject style;
    }
}
