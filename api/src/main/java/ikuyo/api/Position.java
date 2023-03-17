package ikuyo.api;

import com.google.common.base.Objects;

public class Position {
    public double x;
    public double y;

    public Position() {}
    public Position(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double distance(Position pos) {
        return Math.hypot(pos.x - x, pos.y - y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Position position = (Position) o;
        return Double.compare(position.x, x) == 0 && Double.compare(position.y, y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(x, y);
    }
}
