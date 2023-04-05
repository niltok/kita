package ikuyo.manager.behaviors;

import ikuyo.api.behaviors.Behavior;
import ikuyo.manager.api.CommonContext;

import java.util.Objects;
import java.util.Set;

public class PageBehavior implements Behavior<CommonContext> {
    public static Set<String> supportedPage = Set.of("transfer", "starMap", "techTrainer");
    public static Set<String> stationPage = Set.of("cargoHold");
    @Override
    public void update(CommonContext context) {
        context.updated().users().forEach(id -> {
            var state = context.getState(id);
            if (state == null) return;
            var toggleMsgs = state.events.get("page.toggle");
            if (toggleMsgs == null || toggleMsgs.isEmpty() || !state.allowOperate()) return;
            var msg = toggleMsgs.get(toggleMsgs.size() - 1);
            var page = msg.getString("page");
            if (Objects.equals(state.page, page) || !supportedPage.contains(page)) {
                state.setPage("");
            } else  {
                state.setPage(page);
            }
        });
    }
}
