package ikuyo.api.renderers;

import com.fasterxml.jackson.core.JsonProcessingException;
import ikuyo.api.datatypes.BaseContext;
import ikuyo.api.datatypes.UIElement;
import ikuyo.utils.AsyncHelper;
import ikuyo.utils.DataStatic;
import ikuyo.utils.WindowSum;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static ikuyo.api.behaviors.Behavior.windowSize;

public interface UIRenderer<T> extends AsyncHelper {
    void renderUI(T context, Map<Integer, Queue<UIElement>> result);

    static Queue<UIElement> emptyQueue() {
        return new ConcurrentLinkedQueue<>();
    }

    class Composite<T extends BaseContext> implements Renderer<T> {
        UIRenderer<T>[] renderers;
        public Map<String, WindowSum> profilers = new HashMap<>();
        @SafeVarargs
        public Composite(UIRenderer<T>... renderers) {
            this.renderers = renderers;
        }
        @Override
        public JsonObject render(T context) {
            Map<Integer, Queue<UIElement>> map = new ConcurrentHashMap<>();
            context.parallelFor(Arrays.stream(renderers), renderer -> {
                double startTime = System.nanoTime();
                try {
                    renderer.renderUI(context, map);
                } catch (Exception e) {
                    System.err.println(e.getLocalizedMessage());
                    e.printStackTrace();
                } finally {
                    profilers.computeIfAbsent(renderer.getClass().getSimpleName(), i -> new WindowSum(windowSize))
                            .put(System.nanoTime() - startTime);
                }
            });
            JsonObject res = new JsonObject(new ConcurrentHashMap<>());
            map.entrySet().stream().parallel().forEach(entry -> {
                try {
                    res.put(String.valueOf(entry.getKey()), DataStatic.mapper.writeValueAsString(
                                    new UIElement("div", entry.getValue().toArray(UIElement[]::new))
                                            .appendClass("absolute", "fullscreen", "pointer-pass")
                            ));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            });
            return res;
        }
    }
}
