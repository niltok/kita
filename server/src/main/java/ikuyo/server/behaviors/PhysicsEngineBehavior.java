package ikuyo.server.behaviors;

import ikuyo.api.StarInfo;
import ikuyo.api.behaviors.Behavior;
import ikuyo.server.api.BehaviorContext;
import ikuyo.server.api.PhysicsEngine;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.Force;
import org.dyn4j.geometry.Vector2;

import java.util.Map;
import java.util.Objects;

public class PhysicsEngineBehavior  implements Behavior<BehaviorContext> {
    @Override
    public void update(BehaviorContext context) {
        PhysicsEngine PE = context.common().engine();

        if (context.common().updated().init().get())
            PE.Initialize(context.common().star());

        PE.EngineStep(1);

        for (var user: context.common().star().starInfo().starUsers.entrySet()) {
            if (user.getValue().online) {
                Body body = PE.users.get(user.getKey()).getValue();
                if (/*!Objects.equals(body.getChangeInPosition(), new Vector2(0, 0))*/ true) {
                    var pos = context.common().star().starInfo().starUsers.get(user.getKey());
                    pos.x = body.getWorldCenter().x;
                    pos.y = body.getWorldCenter().y;
//                        System.out.println("{EngineBehavior} [x]: %f, [y]: %f".formatted(body.getWorldCenter().x, body.getWorldCenter().y));
                    context.common().updated().users().add(user.getKey());

                }
            }
        }
    }
}
