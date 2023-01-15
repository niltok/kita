package ikuyo.utils;

import io.vertx.core.buffer.Buffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DataStaticTest {
    @Test
    public void testGzip() {
        assertEquals(Buffer.buffer("test"),
                DataStatic.gzipDecode(DataStatic.gzipEncode(Buffer.buffer("test"))));
    }
}
