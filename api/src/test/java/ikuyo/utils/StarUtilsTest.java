package ikuyo.utils;

import ikuyo.api.Position;
import org.dyn4j.geometry.Vector2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


public class StarUtilsTest {
    @Test
    public void printBlock() {
        System.out.println(StarUtils.printBlock(6771));
    }
    @Test
    public void Teat_area() {
        int num = 0, error = 0;
        int testArea = StarUtils.areaNum;
        for (int i = 0; i < 20; i++) {
            List<Integer> list = StarUtils.getBlocksAt(i);
            for (var index: list) {
                num++;
                int reaIndex = StarUtils.realIndexOf(index);
//                System.out.println(i + "," + StarUtils.getAreaOf(reaIndex) + ";index: " + reaIndex);

                if (StarUtils.getAreaOf(reaIndex) != i) {
                    error++;
                    System.out.println(i + "," + StarUtils.getAreaOf(reaIndex) + ";realIndex: " + reaIndex);
                }

//                Assertions.assertEquals(StarUtils.getAreaOf(reaIndex), i);
            }
        }

        System.out.println(num + "," + error);

    }

    @Test
    public void Teat_singleArea() {
        List<Integer> list = StarUtils.areasAround(292.2, -506.2, 20);
        list = StarUtils.getBlocksAt(2068);
        System.out.println(StarUtils.getAreaOf(StarUtils.realIndexOf(292.2, -506.2)));

    }

    @Test
    public void Test_realIndexOf() {
        int testTier = 1000;
        int error = 0;
        for (int index = 0; index < testTier * (testTier+1) * 3; index++) {
            Position pos = StarUtils.positionOf(index);
            for (int i = 0; i < 6; i++) {
                var v = new Vector2(pos.x, pos.y)
                        .add(new Vector2(0.499, 0).rotate(i * Math.PI / 3));
                if (index != StarUtils.realIndexOf(v.x, v.y)) {
                    error++;
//                    int tier = StarInfo.tierOf(index);
//                    System.out.printf("[index]: %d, [cul]: %d, [tier]: %d, [percent]: %f%n",
//                            index, StarInfo.realIndexOf(pos.x, pos.y), tier, (index - tier * (tier - 1) * 3 - 1) / (double) tier);
                }
            }
        }
        System.out.printf("[totalNumber]: %d, [ErrorNumber]: %d%n", testTier*(testTier+1)*3*6, error);
    }

    @Test
    public void Test_newRealIndexOf() {
        int testTier = 1000;

        Position pos = StarUtils.positionOf(0);
        for (int i = 0; i < 6; i++) {
            var v = new Vector2(pos.x, pos.y)
                    .add(new Vector2(0.5, 0).rotate(i * Math.PI / 3));
            Assertions.assertEquals(StarUtils.realIndexOf(v.x, v.y), 0);
        }

        for (int index = 1; index < testTier * (testTier+1) * 3; index++) {
            pos = StarUtils.positionOf(index);
            for (int i = 0; i < 6; i++) {
                var v = new Vector2(pos.x, pos.y)
                        .add(new Vector2(0.4999, 0).rotate(i * Math.PI / 3));
                Assertions.assertEquals(StarUtils.realIndexOf(v.x, v.y), index);
            }
        }
    }

    @Test
    public void Test_singleBlock() {
        int index = 1;
        Position pos = StarUtils.positionOf(index);
        pos.y -= 0.49;
        System.out.printf("[index]: %d, [tier]: %d, [x]: %f, [y]: %f, [angle]: %f%n",
                index, StarUtils.tierOf(index), pos.x, pos.y, (Math.atan2(pos.y, pos.x) + Math.PI * 2) % (Math.PI * 2));
        System.out.printf("Position at [%d] is : [%d]%n", index, StarUtils.realIndexOf(pos.x, pos.y));
//        double x = 0.0, y = 0.0;
//        System.out.printf("Position at [%f,%f] is : [%d]%n%n", x, y, StarInfo.realIndexOf(x, y));
    }

    @Test
    public void Test_tierOf() {
        int testTier = 1000;
        for (int tier = 0; tier < testTier; tier++) {
            for (int i = (tier - 1) * tier * 3 + Math.min(tier, 1); i < tier * (tier + 1) * 3 + 1; i++) {
                Assertions.assertEquals(StarUtils.tierOf(i), tier);
            }
        }
    }
}
