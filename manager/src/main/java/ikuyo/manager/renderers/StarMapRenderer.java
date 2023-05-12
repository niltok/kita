package ikuyo.manager.renderers;

import ikuyo.api.datatypes.UIElement;
import ikuyo.api.entities.Star;
import ikuyo.api.renderers.UIRenderer;
import ikuyo.manager.api.CommonContext;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

public class StarMapRenderer implements UIRenderer<CommonContext> {
    static final double displayScale = 20;
    record StarResult(Star base, Star user, Star[] stars) {}
    @Override
    public void renderUI(CommonContext context, Map<Integer, Queue<UIElement>> result) {
        var fs = new HashMap<Integer, Future<StarResult>>();
        for (Integer id : context.updated().users()) {
            var state = context.getState(id);
            if (state == null || !"starMap".equals(state.page) || state.pageEdge < 2) continue;
            var ui = result.computeIfAbsent(id, i -> UIRenderer.emptyQueue());
            fs.put(id, async(() -> {
                var user = async(() -> Star.getSummery(context.sql(), state.user.star()));
                var summery = Star.getSummery(context.sql(), state.starFocus);
                assert summery != null;
                return new StarResult(summery, await(user), Star.query(context.sql(), state.user.universe(),
                        summery.x() - Star.viewRange, summery.x() + Star.viewRange,
                        summery.y() - Star.viewRange, summery.y() + Star.viewRange));
            }));
        }
        await(CompositeFuture.all(fs.values().stream().map(x -> (Future)x).toList()));
        fs.forEach((id, fut) -> {
            var res = await(fut);
            result.get(id).add(new UIElement("div", Arrays.stream(res.stars())
                    .map(star -> renderStar(res.base(), star, res.user()))
                    .toArray(UIElement[]::new))
                    .appendClass("popout-container", "background"));
        });
    }

    private static UIElement renderStar(Star base, Star star, Star user) {
        double bx = star.x() - base.x(), by = star.y() - base.y();
        double ux = star.x() - user.x(), uy = star.y() - user.y();
        var boxStyle = JsonObject.of(
                "top", "calc(50%% + %fpx + 2px)".formatted(displayScale * by),
                "left", "calc(50%% + %fpx + 2px)".formatted(displayScale * bx));
        var isUser = user.index() == star.index();
        var isBase = base.index() == star.index();
        var dotStyle = JsonObject.of();
        if (isUser) {
            dotStyle.put("background-color", "green");
        } else {
            dotStyle.put("background-color", "red");
        }
        JsonObject callback;
        if (!(isUser && isBase)) {
            boxStyle.put("cursor", "pointer");
            if (isBase) callback = JsonObject.of("type", "user.move.star", "target", star.index());
            else callback = JsonObject.of("type", "starMap.focus", "target", star.index());
        } else {
            boxStyle.put("cursor", "default");
            callback = JsonObject.of();
        }
        var text = isUser ? star.name() : "%s(%.1fly)".formatted(star.name(), Math.hypot(ux, uy));
        return new UIElement.Callback("div", callback,
                new UIElement("div").appendClass("starmap-dot").withStyle(dotStyle),
                new UIElement.Text(text)).appendClass("hover-label", "absolute").withStyle(boxStyle);
    }
}
