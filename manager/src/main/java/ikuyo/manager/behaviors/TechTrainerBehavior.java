package ikuyo.manager.behaviors;

import ikuyo.api.behaviors.Behavior;
import ikuyo.manager.api.BehaviorContext;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Tuple;

import java.util.ArrayList;
import java.util.List;

public class TechTrainerBehavior implements Behavior<BehaviorContext> {
    @Override
    public void update(BehaviorContext context) {
        switch (context.event().getString("type")) {
            case "techTrainer.toggle" -> {
                context.context().updated().users().add(context.id());
                var state = context.context().userState().get(context.id());
                state.techTrainerDisplay = !state.techTrainerDisplay;
            }
            case "techTrainer.add" -> {
                context.context().updated().users().add(context.id());
                var tech = context.event().getString("tech");
                var user = context.context().userState().get(context.id()).user;
                var tree = user.techTree();
                var queue = new ArrayList<>(List.of(tree.getTrainQueue()));
                queue.add(tech);
                tree.setTrainQueue(queue, true);
                context.context().sql().preparedQuery("""
                    update "user" set tech_tree = $1 where index = $2
                    """).execute(Tuple.of(tree.toString(), context.id()));
                context.context().eventBus().send("star." + user.star(),
                        JsonObject.of("type", "user.update", "id", context.id()));
            }
        }
    }
}
