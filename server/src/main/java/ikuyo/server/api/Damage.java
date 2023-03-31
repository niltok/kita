package ikuyo.server.api;

public class Damage implements Cloneable{
    public boolean ifShieldDamage = true;
    public boolean ifHpDamage = true;
    public boolean ifBreakBlock = true;
    public boolean ifSanDamage = false;
    public boolean ifPersistentDamage = false;
    private int damageFrequency = 0;
    private int frameCount = 0;
    public double normalDamage = 0;
    public double sanDamage = 0;
    public double shieldOnlyDamage = 0;
    public double HpOnlyDamage = 0;

/*    public Damage(Damage damage) {
        for (var field : this.getClass().getFields()) {
            try {
                field.setAccessible(true);
                field.set(field.getType(), damage.getClass().getField(field.getName()));
            } catch (Exception e) {
                System.out.println("{Damage}: Error!");
            }
        }

    }*/

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
