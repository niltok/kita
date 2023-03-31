package ikuyo.manager.renderers;

import ikuyo.api.UIElement;
import ikuyo.api.renderers.UIRenderer;
import ikuyo.manager.api.CommonContext;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class TransferRenderer implements UIRenderer<CommonContext> {
    @Override
    public void renderUI(CommonContext context, Map<Integer, List<UIElement>> result) {
        for (Integer id : context.updated().users()) {
            var ui = result.computeIfAbsent(id, i -> new ArrayList<>());
            if (!"transfer".equals(context.userState().get(id).page)) {
                ui.add(new UIElement("div").withClass("placeholder"));
                continue;
            }
            ui.add(new UIElement("div", transferAnimation(), new UIElement.Text("Transferring..."))
                    .withClass("flex-center", "fullscreen", "transfer-background"));
        }
    }

    static final int lineCount = 40;
    UIElement transferAnimation() {
        var rand = new Random();
        var res = new ArrayList<UIElement>();
        for (var i = 0; i < lineCount; i++) {
            var time = rand.nextDouble(1, 3);
            var flyBlock = new UIElement("div")
                    .withClass("transfer-line")
                    .withStyle(JsonObject.of(
                            "animationDelay", "%fs".formatted(rand.nextDouble(3)),
                            "animationDuration", "%fs".formatted(time),
                            "width", "%f%%".formatted(time * 3),
                            "height", "%f%%".formatted(rand.nextDouble(50, 100))));
            var style = JsonObject.of("transform",
                    "rotateZ(%fdeg) rotateY(-45deg) translateX(10px)".formatted(
                            rand.nextDouble(360)));
            res.add(new UIElement("div", flyBlock)
                    .withClass("transfer-box")
                    .withStyle(style));
        }
        return new UIElement("div", res.toArray(UIElement[]::new))
                .withClass("fullscreen", "transfer-background", "no-overflow", "absolute")
                .withStyle(JsonObject.of("perspective", "100px"));
    }
}
