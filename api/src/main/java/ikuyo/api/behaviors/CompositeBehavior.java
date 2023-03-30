package ikuyo.api.behaviors;

public class CompositeBehavior<T> implements Behavior<T> {
    Behavior<T>[] behaviors;
    @SafeVarargs
    public CompositeBehavior(Behavior<T>... behaviors) {
        this.behaviors = behaviors;
    }

    @Override
    public void update(T context) {
        for (Behavior<T> behavior : behaviors) {
            try {
                behavior.update(context);
            } catch (Exception e) {
                System.err.println(e.getLocalizedMessage());
                e.printStackTrace();
            }
        }
    }
}
