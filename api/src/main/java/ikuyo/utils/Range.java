package ikuyo.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class Range {
    ArrayList<Double> list;
    boolean test;
    public Range(long seed) {
        list = new ArrayList<>();
        Random random = new Random(seed);
        double length = 0.0;
        while (true) {
            double r = random.nextDouble() * 0.15 + 0.05;
            length += r;
            if (length <= 1.0) {
                list.add(length);
                list.add(random.nextDouble() * 0.7 + 0.3);
            }
            else break;
        }
        list.add(1.0);
        list.add(random.nextDouble());
    }

    public double Random(double percent) {
        test = false;
        double random = 0.0, range = 0.0;
        Iterator<Double> It = list.iterator();
        while (percent >= range) {
            range = It.next();
            random = It.next();
        }
        double round = 0.02;
        if (Math.abs(range -percent) < round) {
            test = true;
            if ( !It.hasNext() ) It = list.iterator();
            It.next();
            double nextRandom = It.next();
            random = random + (nextRandom - random)
                    * (0.5 - Math.atan((range - percent) * 6 / round - 3) / Math.PI);
        }

        return random;
    }
}
