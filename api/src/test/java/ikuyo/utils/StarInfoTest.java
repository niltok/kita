package ikuyo.utils;
import ikuyo.api.Position;
import org.junit.jupiter.api.Test;

import ikuyo.api.StarInfo;

public class StarInfoTest {
    @Test
    public void Test_realIndexOf() {
        int starttier = 2;
        int testtier = 500;
        int error = 0;
        for (int i = starttier*(starttier+1)*3; i < testtier*(testtier+1)*3; i++) {
            Position pos = StarInfo.posOf(i);
            if (i != StarInfo.realIndexOf(pos.x, pos.y)) {
                error++;
                System.out.printf("[i]: %d, [cul]: %d%n", i, StarInfo.realIndexOf(pos.x, pos.y));
            }
        }
        System.out.printf("[totalNumber]: %d, [ErrorNumber]: %d%n", testtier*(testtier+1)*3 - starttier*(starttier+1)*3, error);
    }
    @Test
    public void Test_singleblock() {
        int index = 17;
        Position pos = StarInfo.posOf(index);
        System.out.printf("[index]: %d, [tier]: %d, [x]: %f, [y]: %f, [angle]: %f%n",
                index, StarInfo.tierOf(index), pos.x, pos.y, (Math.atan2(pos.y, pos.x) + Math.PI * 2) % (Math.PI * 2));
        System.out.printf("Position at [%d] is : [%d]%n", index, StarInfo.realIndexOf(pos.x, pos.y));
        double x = 1.000000, y = -1.732051;
        System.out.printf("Position at [%f,%f] is : [%d]%n%n", x, y, StarInfo.realIndexOf(x, y));
    }
}
