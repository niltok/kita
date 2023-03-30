package ikuyo.api.techtree;

public non-sealed class TechLevel implements TechDependency {
    public TechItem techItem;
    public long level;

    public TechLevel(TechItem techItem, long level) {
        this.techItem = techItem;
        this.level = level;
    }
}
