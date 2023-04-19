package ikuyo.api.behaviors;

import ikuyo.utils.WindowSum;

import java.util.HashMap;
import java.util.Map;

public class CompositeBehavior<T> implements Behavior<T> {
    Behavior<T>[] behaviors;
    public Map<String, WindowSum> profilers = new HashMap<>();
    @SafeVarargs
    public CompositeBehavior(Behavior<T>... behaviors) {
        this.behaviors = behaviors;
    }

    @Override
    public void update(T context) {
        for (Behavior<T> behavior : behaviors) {
            double startTime = System.nanoTime();
            try {
                behavior.update(context);
            } catch (Exception e) {
                System.err.println(e.getLocalizedMessage());
                e.printStackTrace();
            } finally {
                profilers.computeIfAbsent(behavior.getClass().getSimpleName(), i -> new WindowSum(windowSize))
                        .put(System.nanoTime() - startTime);
            }
        }
    }
}
