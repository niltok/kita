package ikuyo.api.hooks;

import com.fasterxml.jackson.annotation.JsonBackReference;
import ikuyo.api.spaceships.Spaceship;

public class SpaceshipHook extends AbstractHook {
    @JsonBackReference
    protected Spaceship spaceship;
    public SpaceshipHook() {}
    public SpaceshipHook(Spaceship spaceship) {
        this.spaceship = spaceship;
    }
    public double getShield() {
        return spaceship.getInfo().shieldMax * (1 + reduce(ShieldStatic.addPercent));
    }
}
