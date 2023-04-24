package ikuyo.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class DataStaticTest {
    @Test
    public void testGzip() {
        assertArrayEquals("test".getBytes(),
                DataStatic.gzipDecode(DataStatic.gzipEncode("test".getBytes())));
    }
}
