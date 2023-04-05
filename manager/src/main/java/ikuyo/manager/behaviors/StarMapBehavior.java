package ikuyo.manager.behaviors;

import ikuyo.api.behaviors.Behavior;
import ikuyo.manager.api.CommonContext;

public class StarMapBehavior implements Behavior<CommonContext> {
    @Override
    public void update(CommonContext context) {
        for (var id : context.updated().users()) {
            var state = context.getState(id);
            if (state == null || !"starMap".equals(state.page)) continue;
            if (state.pageEdge == 3) state.starFocus = state.user.star();
            var focusMsgs = state.events.get("starMap.focus");
            if (focusMsgs != null && !focusMsgs.isEmpty()) {
                var msg = focusMsgs.get(focusMsgs.size() - 1);
                state.starFocus = msg.getInteger("target");
            }
        }
    }
}
