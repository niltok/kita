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

//        if (context.common().updated().init().get())
//            for (var user: context.common().users().values()) {
//                engine.Initialize(context.common().star());
//            }
//        for (var id: context.common().updated().users()) {
//            if (!context.common().star().starInfo().starUsers.get(id).online) {
////                Body body = PE.users.get(id);
////                PE.world.removeBody(body);
////                PE.users.remove(id, body);
//            }else {
//                if (PE.users.get(id) == null) {
//                    PE.addUser(Map.entry(id, context.common().star().starInfo().starUsers.get(id)));
//                }
//            }
//        }

        PE.world.step(1);
        for (var user: context.common().star().starInfo().starUsers.entrySet()) {
            if (user.getValue().online) {
                Body body = PE.users.get(user.getKey());
                if (body == null) {
//                    System.out.println("!!!!!NO BODY!!!!! [userid]: %d".formatted(user.getKey()));
                }else {
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
}
