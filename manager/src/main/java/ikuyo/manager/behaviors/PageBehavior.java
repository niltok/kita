package ikuyo.manager.behaviors;

import ikuyo.api.behaviors.Behavior;
import ikuyo.manager.api.BehaviorContext;

import java.util.Objects;
import java.util.Set;

public class PageBehavior implements Behavior<BehaviorContext> {
    public static Set<String> supportedPage = Set.of("transfer", "cargoHold", "techTrainer");
    public static Set<String> stationPage = Set.of("starMap");
    @Override
    public void update(BehaviorContext context) {
        var type = context.event().getString("type");
        switch (type) {
            case "page.toggle" -> {
                var page = context.event().getString("page");
                var state = context.getState(context.id());
                if ("transfer".equals(state.page)) break;
                context.updated().users().add(context.id());
                state.page = Objects.equals(state.page, page) || !supportedPage.contains(page) ? "" : page;
            }
        }
        context.forward().set(!"transfer".equals(context.getState(context.id()).page));
    }
}
