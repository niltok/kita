package ikuyo.utils;

import ikuyo.api.datatypes.StarInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class StarTest {
    @Test
    public void starGenTest() {
        var startTime = System.nanoTime();
        var star = StarInfo.generate(0);
        var size = star.toString().length() / 1024.;
        var sizeCompressed = star.toBuffer().length() / 1024.;
        var runTime = System.nanoTime() - startTime;
        System.out.printf("run time(ms): %f%n", runTime / 1000_000.);
        System.out.printf("size(KB): %f(%f compressed)%n", size, sizeCompressed);
    }

    @Test
    public void starInfoGenTest() throws IOException {
        System.out.println(StarInfo.genStarInfo("{}", new String[]{"{}", "{}"}).toString());
    }
}
