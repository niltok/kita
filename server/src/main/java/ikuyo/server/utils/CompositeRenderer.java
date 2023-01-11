package ikuyo.server.utils;

public class CompositeRenderer extends AbstractRenderer {
    Renderer[] renderers;
    public CompositeRenderer(Renderer ...renderers) {
        this.renderers = renderers;
    }

    @Override
    public void init(Context context) {
        super.init(context);
        for (Renderer renderer : renderers) {
            renderer.init(context);
        }
    }

    @Override
    public void render() {
        for (Renderer renderer : renderers) {
            renderer.render();
        }
    }
}
