package ikuyo.server.utils;

public class AbstractRenderer implements Renderer {
    public Context context;

    @Override
    public void init(Context context) {
        this.context = context;
    }

    @Override
    public void render() {}
}
