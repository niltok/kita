package ikuyo.server.api;

import org.dyn4j.dynamics.Body;
import org.dyn4j.geometry.Transform;
import org.dyn4j.geometry.Vector2;

public class KitasBody extends Body{
    private boolean bearTheGravity = false;
    private boolean isRotatable = false;
    private boolean fixRotation = false;
    public double lastAngle = 0.0;
    public double angle = 0.0;
    public String controlType = "walk";

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
            Transform t = this.getTransform();
            if (Math.abs(t.getRotationAngle() - angle) > 0.02)
                t.setRotation(angle);
            this.setAngularVelocity(0);
        }
        else this.getTransform().setRotation(
                this.getTransform().getRotationAngle() - this.lastAngle + this.angle);
        this.lastAngle = this.angle;
    }

    public void preprocess() {
        this.setBearTheGravity(!this.getBearTheGravity()
                        || !(this.getWorldCenter().distance(0, 0) >= PhysicsEngine.starR));
                this.applyGravity();
                this.updateRotation();
    }

    public void setControlType(String type) {
        if (this.controlType.equals(type)) return;
        this.controlType = type;
        if (type.equals("fly")) {
            this.setAngularVelocity(0);
            this.setGravityScale(0.01);
            this.setFixRotation(false);
        }
        else if (type.equals("walk")) {
            this.setGravityScale(1);
            this.setFixRotation(true);
        }
    }
}

