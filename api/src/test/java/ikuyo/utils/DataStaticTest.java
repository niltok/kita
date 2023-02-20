package ikuyo.utils;

import io.vertx.core.buffer.Buffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DataStaticTest {
    @Test
    public void testGzip() {
        assertEquals("test".getBytes(),
                DataStatic.gzipDecode(DataStatic.gzipEncode("test".getBytes())));
    }
}
