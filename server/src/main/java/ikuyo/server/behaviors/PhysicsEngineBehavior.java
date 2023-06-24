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

        for (var userdata: context.engine().users.values())
            userdata.preprocess(context.engine());

        PE.EngineStep(1);

        for (var user: context.star().starInfo().starUsers.entrySet()) {
            if (user.getValue().online) {
                UserEngineData userdata = PE.users.get(user.getKey());
                if (!userdata.getBody().getChangeInPosition().equals(0, 0) || context.updated().init()) {
                    var userInfo = context.star().starInfo().starUsers.get(user.getKey());
                    userInfo.x = userdata.getBody().getWorldCenter().x;
                    userInfo.y = userdata.getBody().getWorldCenter().y;
                    userInfo.rotation = userdata.getBody().getTransform().getRotationAngle() + Math.PI / 2;
//                        System.out.println("{EngineBehavior} [x]: %f, [y]: %f".formatted(body.getWorldCenter().x, body.getWorldCenter().y));
                    userInfo.cameraX = userdata.getCamera().getWorldCenter().x;
                    userInfo.cameraY = userdata.getCamera().getWorldCenter().y;
                    context.updated().users().add(user.getKey());
                }
            }
        }
    }
}
