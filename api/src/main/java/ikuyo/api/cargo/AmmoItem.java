package ikuyo.api.cargo;

public class AmmoItem extends CargoItem {
    public long liveTime;
    public AmmoItem(String displayName, String description, double volume, long liveTime) {
        super(displayName, description, volume);
        this.liveTime = liveTime;
    }
    public static AmmoItem get(String index) {
        try {
            return (AmmoItem) CargoStatic.class.getField(index).get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            return null;
        }
    }
}
