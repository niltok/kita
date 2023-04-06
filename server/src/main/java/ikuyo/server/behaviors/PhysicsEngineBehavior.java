package ikuyo.server.behaviors;

import ikuyo.api.behaviors.Behavior;
import ikuyo.server.api.CommonContext;
import ikuyo.server.api.PhysicsEngine;
import ikuyo.server.api.UserEngineData;

public class PhysicsEngineBehavior  implements Behavior<CommonContext> {
    @Override
    public void update(CommonContext context) {
        PhysicsEngine PE = context.engine();

        if (context.updated().init())
            PE.Initialize(context.star());

        PE.EngineStep(1);

        for (var user: context.star().starInfo().starUsers.entrySet()) {
            if (user.getValue().online) {
                UserEngineData userData = PE.users.get(user.getKey());
                if (!userData.getBody().getChangeInPosition().equals(0, 0) || context.updated().init()) {
                    var userInfo = context.star().starInfo().starUsers.get(user.getKey());
                    userInfo.x = userData.getBody().getWorldCenter().x;
                    userInfo.y = userData.getBody().getWorldCenter().y;
                    userInfo.rotation = userData.getBody().getTransform().getRotationAngle() + Math.PI / 2;
//                        System.out.println("{EngineBehavior} [x]: %f, [y]: %f".formatted(body.getWorldCenter().x, body.getWorldCenter().y));
                    userInfo.cameraX = userData.getCamera().getWorldCenter().x;
                    userInfo.cameraY = userData.getCamera().getWorldCenter().y;
                    context.updated().users().add(user.getKey());
                }
            }
        }
    }
}
