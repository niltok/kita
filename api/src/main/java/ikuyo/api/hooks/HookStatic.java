package ikuyo.api.hooks;

import ikuyo.utils.ItemUtils;

@ItemUtils.ItemInject({ShieldStatic.class})
public class HookStatic {
    static {
        ItemUtils.setFieldName(HookStatic.class, HookPoint.class, "name");
    }
}
