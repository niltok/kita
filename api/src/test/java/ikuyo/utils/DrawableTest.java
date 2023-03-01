package ikuyo.utils;

import ikuyo.api.Drawable;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DrawableTest {
    @Test
    public void jsonTest() {
        var d = new Drawable.Sprite();
        assertEquals(JsonObject.mapFrom(d), d.toJson());
    }
}
