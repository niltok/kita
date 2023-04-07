package ikuyo.api.hooks;

import java.util.List;

public class HookContext {
    public List<HookToken> hookTokens;
    public AbstractHook hook;
    public HookContext(List<HookToken> hookTokens, AbstractHook hook) {
        this.hookTokens = hookTokens;
        this.hook = hook;
    }
}
