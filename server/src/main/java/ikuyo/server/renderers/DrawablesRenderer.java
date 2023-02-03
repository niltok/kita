package ikuyo.server.renderers;

import com.google.common.hash.Hashing;
import ikuyo.api.Drawable;
import ikuyo.api.Star;
import ikuyo.server.api.Renderer;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface DrawablesRenderer extends Renderer {
    default JsonObject render(Star star) {
        var drawables = new ArrayList<Drawable>();
        renderDrawables(star, drawables);
        return genDrawables(drawables);
    }

    private static JsonObject genDrawables(ArrayList<Drawable> drawables) {
        var encoder = Base64.getEncoder();
        return new JsonObject(drawables.stream().map(JsonObject::mapFrom).collect(Collectors.toMap(
                json -> encoder.encodeToString(Hashing.sha256()
                        .hashString(json.toString(), Charset.defaultCharset()).asBytes()),
                Function.identity(),
                (s, a) -> {
                    LoggerFactory.getLogger("DrawablesRenderer$genDrawables").warn(JsonObject.of(
                            "type", "drawable hash conflicted",
                            "prev", JsonObject.mapFrom(s),
                            "new", JsonObject.mapFrom(a)));
                    return s;
                }
        )));
    }

    void renderDrawables(Star star, List<Drawable> drawables);

    class Composite implements Renderer {
        DrawablesRenderer[] renderers;
        public Composite(DrawablesRenderer... renderers) {
            this.renderers = renderers;
        }

        @Override
        public JsonObject render(Star star) {
            var drawables = new ArrayList<Drawable>();
            for (DrawablesRenderer renderer : renderers) {
                renderer.renderDrawables(star, drawables);
            }
            return genDrawables(drawables);
        }
    }
}
