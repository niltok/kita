package ikuyo.manager.behaviors;

import ikuyo.api.behaviors.Behavior;
import ikuyo.api.techtree.TechItem;
import ikuyo.api.techtree.TechLevel;
import ikuyo.manager.api.BehaviorContext;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Tuple;

public class TechTrainerBehavior implements Behavior<BehaviorContext> {
    @Override
    public void update(BehaviorContext context) {
        switch (context.event().getString("type")) {
            case "techTrainer.toggle" -> {
                context.context().updated().users().add(context.id());
                var state = context.context().userState().get(context.id());
                if (!"transfer".equals(state.page))
                    state.page = "techTrainer".equals(state.page) ? "" : "techTrainer";
            }
            case "techTrainer.train" -> {
                context.context().updated().users().add(context.id());
                var tech = TechItem.get(context.event().getString("tech"));
                var level = Long.parseLong(context.event().getString("level"));
                var user = context.context().userState().get(context.id()).user;
                var tree = user.techTree();
                tree.train(new TechLevel(tech, level));
                context.context().sql().preparedQuery("""
                    update "user" set tech_tree = $1 where index = $2
                    """).execute(Tuple.of(tree.toString(), context.id()));
                context.context().eventBus().send("star." + user.star(),
                        JsonObject.of("type", "user.update", "id", context.id()));
            }
        }
    }
}
