package ikuyo.api.hooks;

public final class HookToken {
    public final String hookPoint;
    public final String hookId;

    public HookToken() {
        this("", "");
    }

    public HookToken(String hookPoint, String hookId) {
        this.hookPoint = hookPoint;
        this.hookId = hookId;
    }
}
