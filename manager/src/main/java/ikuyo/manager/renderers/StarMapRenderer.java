package ikuyo.manager.renderers;

import ikuyo.api.Star;
import ikuyo.api.UIElement;
import ikuyo.manager.api.RendererContext;
import ikuyo.utils.AsyncHelper;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.apache.commons.math3.util.Pair;

import java.util.*;

public class StarMapRenderer implements UIRenderer, AsyncHelper {
    static final double displayScale = 20;
    @Override
    public void renderUI(RendererContext context, Map<Integer, List<UIElement>> result) {
        var fs = new HashMap<Integer, Future<Pair<Star, Star[]>>>();
        for (Integer id : context.common().updated().users()) {
            var ui = result.computeIfAbsent(id, i -> new ArrayList<>());
            if (!context.common().userState().get(id).mapDisplay) {
                ui.add(new UIElement("div").withClass("placeholder"));
                continue;
            }
            var user = context.common().userState().get(id).user;
            var client = context.common().sql();
            fs.put(id, async(() -> {
                var summery = Star.getSummery(client, user.star());
                assert summery != null;
                return Pair.create(summery, Star.query(client, user.universe(),
                        summery.x() - Star.viewRange, summery.x() + Star.viewRange,
                        summery.y() - Star.viewRange, summery.y() + Star.viewRange));
            }));
        }
        await(CompositeFuture.all(fs.values().stream().map(x -> (Future)x).toList()));
        fs.forEach((id, fut) -> {
            var pair = await(fut);
            result.get(id).add(new UIElement("div", Arrays.stream(pair.getSecond()).map(star -> {
                return renderStar(pair.getFirst(), star);
            }).toArray(UIElement[]::new)).withClass("starmap-container", "background"));
        });
    }

    private static UIElement renderStar(Star base, Star star) {
        var dx = star.x() - base.x();
        var dy = star.y() - base.y();
        var boxStyle = JsonObject.of(
                "top", "calc(50%% + %fpx + 2px)".formatted(displayScale * dy),
                "left", "calc(50%% + %fpx + 2px)".formatted(displayScale * dx));
        var dotStyle = JsonObject.of();
        if (base.index() == star.index()) dotStyle.put("background-color", "yellow");
        var text = base.index() == star.index() ?
                star.name() :
                "%s(%.1fly)".formatted(star.name(), Math.hypot(dx, dy));
        return new UIElement("div",
                new UIElement("div").withClass("starmap-dot").withStyle(dotStyle),
                new UIElement.Text(text)).withClass("starmap-label").withStyle(boxStyle);
    }
}
