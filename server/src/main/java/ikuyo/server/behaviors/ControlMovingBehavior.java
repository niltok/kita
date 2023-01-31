package ikuyo.server.behaviors;

public class ControlMovingBehavior extends AbstractBehavior {
    @Override
    public void update() {
        var speed = 5; // TODO: moving logic
        context.userKeyInputs().forEach((id, input) -> {
            var pos = context.star().starInfo().starUsers.get(id);
            if (input.up > 0) pos.y -= speed;
            if (input.down > 0) pos.y += speed;
            if (input.left > 0) pos.x -= speed;
            if (input.right > 0) pos.x += speed;
        });
    }
}
