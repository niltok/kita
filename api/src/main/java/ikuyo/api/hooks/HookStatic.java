package ikuyo.api.hooks;

import ikuyo.utils.ItemUtils;

@ItemUtils.ItemInject({ShieldStatic.class})
@ItemUtils.ItemTarget(HookPoint.class)
public class HookStatic {
    static {
        ItemUtils.setFieldName(HookStatic.class);
    }
}
