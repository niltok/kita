package ikuyo.server.behaviors;

import ikuyo.api.behaviors.Behavior;
import ikuyo.server.api.CommonContext;
import ikuyo.server.api.PhysicsEngine;
import org.dyn4j.dynamics.Body;

public class PhysicsEngineBehavior  implements Behavior<CommonContext> {
    @Override
    public void update(CommonContext context) {
        PhysicsEngine PE = context.engine();

        if (context.updated().init().get())
            PE.Initialize(context.star());

        PE.EngineStep(1);

        for (var user: context.star().starInfo().starUsers.entrySet()) {
            if (user.getValue().online) {
                Body body = PE.users.get(user.getKey()).getValue();
                if (!body.getChangeInPosition().equals(0, 0)) {
                    var userInfo = context.star().starInfo().starUsers.get(user.getKey());
                    userInfo.x = body.getWorldCenter().x;
                    userInfo.y = body.getWorldCenter().y;
                    userInfo.rotation = body.getTransform().getRotationAngle() + Math.PI / 2;
//                        System.out.println("{EngineBehavior} [x]: %f, [y]: %f".formatted(body.getWorldCenter().x, body.getWorldCenter().y));
                    context.updated().users().add(user.getKey());

                }
            }
        }
    }
}
