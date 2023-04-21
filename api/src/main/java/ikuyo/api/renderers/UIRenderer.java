package ikuyo.api.renderers;

import ikuyo.api.datatypes.BaseContext;
import ikuyo.api.datatypes.UIElement;
import ikuyo.utils.AsyncHelper;
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
            Arrays.stream(renderers).forEach(renderer -> {
                long startTime = System.nanoTime();
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
            long startTime = System.nanoTime();
            JsonObject res = new JsonObject(new ConcurrentHashMap<>());
            map.entrySet().stream().parallel().forEach(entry -> {
                res.put(String.valueOf(entry.getKey()), JsonObject.mapFrom(
                                new UIElement("div", entry.getValue().toArray(UIElement[]::new))
                                        .appendClass("absolute", "fullscreen", "pointer-pass")
                        ));
            });
            profilers.computeIfAbsent("UIRenderer.Compose", i -> new WindowSum(windowSize))
                    .put(System.nanoTime() - startTime);
            return res;
        }
    }
}
