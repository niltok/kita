package ikuyo.utils;

import io.vertx.core.buffer.Buffer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public interface DataStatic {
    static Buffer gzipEncode(Buffer data) {
        var out = new ByteArrayOutputStream();
        try {
            var gzip = new GZIPOutputStream(out);
            gzip.write(data.getBytes());
            gzip.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Buffer.buffer(out.toByteArray());
    }

    static Buffer gzipDecode(Buffer zip) {
        var in = new ByteArrayInputStream(zip.getBytes());
        try {
            var gzip = new GZIPInputStream(in);
            return Buffer.buffer(gzip.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
