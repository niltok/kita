package ikuyo.utils;

public class WindowSum {
    private double state = 0;
    private final double[] buffer;
    private int pos = 0;
    private boolean init = true;

    public WindowSum(int bufferSize) {
        this.buffer = new double[bufferSize];
    }

    public WindowSum put(double val) {
        state += val - buffer[pos];
        buffer[pos] = val;
        pos = (pos + 1) % buffer.length;
        init &= pos != 0;
        return this;
    }

    public double getSum() {
        return state;
    }

    public double getMean() {
        return getSum() / size();
    }

    public int size() {
        if (init) return pos;
        return buffer.length;
    }
}
