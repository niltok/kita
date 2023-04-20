package ikuyo.api.datatypes;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public class Damage implements Cloneable{
    public boolean ifBreakBlock = false;
    public boolean ifPersistentDamage = false;
    private int damageFrequency = 0;
    private int frameCount = 0;
    public double normalDamage = 0;
    public double sanDamage = 0;
    public double shieldOnlyDamage = 0;
    public double hpOnlyDamage = 0;
    public double range = 0;
    public Damage() {}
    public Damage(double damage) {
        normalDamage = damage;
    }

    public Damage setRange(double range) {
        this.range = range;
        return this;
    }

    public Damage setIfBreakBlock(boolean ifBreakBlock) {
        this.ifBreakBlock = ifBreakBlock;
        return this;
    }

    public void enablePersistDamage(int frequency, boolean atOnce) {
        this.ifPersistentDamage = true;
        this.damageFrequency = frequency;
        if (atOnce) this.frameCount = 0;
        else this.frameCount = frequency;
    };

    public void disablePersistDamage() {
        this.ifPersistentDamage = false;
    }

    public  boolean ifInflict() {
        if (!this.ifPersistentDamage) return true;
        if (this.frameCount == 0) {
            this.frameCount = this.damageFrequency;
            return true;
        }
        this.frameCount--;
        return false;
    }

    @Override
    public Damage clone() {
        try {
            return (Damage) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public static final class Normal extends Damage{
        Normal() {

        }
    }
}
