package ikuyo.utils;
import ikuyo.api.Position;
import org.junit.jupiter.api.Test;

import ikuyo.api.StarInfo;

public class StarInfoTest {
    @Test
    public void Test_realIndexOf() {
        int testTier = 500;
        int error = 0;
        for (int i = 0; i < testTier * (testTier+1) * 3; i++) {
            Position pos = StarInfo.posOf(i);
            pos.x -= 0.49;
            if (i != StarInfo.realIndexOf(pos.x, pos.y)) {
                error++;
                int tier = StarInfo.tierOf(i);
//                System.out.printf("[i]: %d, [cul]: %d, [tier]: %d, [percent]: %f%n",
//                        i, StarInfo.realIndexOf(pos.x, pos.y), tier, (i - tier*(tier-1)*3 - 1) / (double)tier);
            }
        }
        System.out.printf("[totalNumber]: %d, [ErrorNumber]: %d%n", testTier*(testTier+1)*3, error);
    }
    @Test
    public void Test_singleBlock() {
        int index = 0;
        Position pos = StarInfo.posOf(index);
        pos.y -= 0.49;
        System.out.printf("[index]: %d, [tier]: %d, [x]: %f, [y]: %f, [angle]: %f%n",
                index, StarInfo.tierOf(index), pos.x, pos.y, (Math.atan2(pos.y, pos.x) + Math.PI * 2) % (Math.PI * 2));
        System.out.printf("Position at [%d] is : [%d]%n", index, StarInfo.realIndexOf(pos.x, pos.y));
//        double x = 0.0, y = 0.0;
//        System.out.printf("Position at [%f,%f] is : [%d]%n%n", x, y, StarInfo.realIndexOf(x, y));
    }
}
