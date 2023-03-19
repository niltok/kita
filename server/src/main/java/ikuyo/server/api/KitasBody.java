package ikuyo.server.api;

import org.dyn4j.dynamics.Body;
import org.dyn4j.geometry.Vector2;

public class KitasBody extends Body{
    private boolean bearTheGravity = false;
    private boolean isRotatable = false;
    private boolean fixRotation = false;
    public double lastAngle = 0.0;
    public double angle = 0.0;

    public boolean getRotatable() {
        return this.isRotatable;
    }

    public void setRotatable(boolean state) {
        if (state)
            this.isRotatable = true;
        else {
            this.isRotatable = false;
            this.lastAngle = 0.0;
            this.angle = 0.0;
        }
    }

    public void setFixRotation(boolean state) {
        this.fixRotation = state;
    }

    public void setBearTheGravity(boolean state) {
        this.bearTheGravity = state;
    }

    public boolean getBearTheGravity() {
        return this.bearTheGravity;
    }

    public void applyGravity() {
        if (!this.bearTheGravity) return;
        Vector2 pos = this.getWorldCenter();
        this.applyForce(new Vector2(-PhysicsEngine.GravitationalAcc * this.getMass().getMass(), 0)
                .multiply(this.gravityScale).rotate(Math.atan2(pos.y, pos.x)));
    }

    public void updateRotation() {
        if (!this.isRotatable) return;
        this.angle = Math.atan2(this.getWorldCenter().y, this.getWorldCenter().x);
        if (fixRotation) {
            this.getTransform().setRotation(angle);
            this.setAngularVelocity(0);
        }
        else this.getTransform().setRotation(
                this.getTransform().getRotationAngle() - this.lastAngle + this.angle);
        this.lastAngle = this.angle;
    }
}
