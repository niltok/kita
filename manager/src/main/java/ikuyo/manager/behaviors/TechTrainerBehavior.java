package ikuyo.manager.behaviors;

import ikuyo.api.behaviors.Behavior;
import ikuyo.api.techtree.TechItem;
import ikuyo.api.techtree.TechLevel;
import ikuyo.manager.api.CommonContext;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Tuple;

public class TechTrainerBehavior implements Behavior<CommonContext> {
    @Override
    public void update(CommonContext context) {
        context.updated().users().forEach(id -> {
            var state = context.getState(id);
            if (state == null) return;
            var trainMsgs = state.events.get("techTrainer.train");
            if (trainMsgs != null) trainMsgs.forEach(msg -> {
                var tech = TechItem.get(msg.getString("tech"));
                var level = Long.parseLong(msg.getString("level"));
                var user = state.user;
                var tree = user.techTree();
                tree.train(new TechLevel(tech, level));
                context.sql().preparedQuery("""
                    update "user" set tech_tree = $1 where index = $2
                    """).execute(Tuple.of(tree.toString(), id));
                context.eventBus().send("star." + user.star(),
                        JsonObject.of("type", "user.update", "id", id));
            });
        });
    }
}
