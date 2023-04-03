package ikuyo.server.behaviors;

import ikuyo.api.behaviors.Behavior;
import ikuyo.server.api.CommonContext;

import java.util.Objects;
import java.util.Set;

public class PageBehavior implements Behavior<CommonContext> {
    public static Set<String> supportedPage = Set.of("cargoHold");
    @Override
    public void update(CommonContext context) {
        context.updated().users().forEach(id -> {
            var state = context.getState(id);
            var toggleMsg = state.events.get("page.toggle");
            if (toggleMsg == null || toggleMsg.isEmpty()) return;
            var page = toggleMsg.get(toggleMsg.size() - 1).getString("page");
            state.page = Objects.equals(page, state.page) || !supportedPage.contains(page) ? "" : page;
        });
    }
}
