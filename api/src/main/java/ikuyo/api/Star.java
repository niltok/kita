package ikuyo.api;

public record Star(int index, int universe, Block[] blocks) {
    public record StarGroup(int universe, int posX, int posY) {}
}
