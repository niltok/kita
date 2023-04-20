package ikuyo.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.buffer.Buffer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public interface DataStatic {
    static Buffer gzipEncode(byte[] data) {
        var out = new ByteArrayOutputStream();
        try {
            var gzip = new GZIPOutputStream(out);
            gzip.write(data);
            gzip.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Buffer.buffer(out.toByteArray());
    }

    static byte[] gzipDecode(Buffer zip) {
        var in = new ByteArrayInputStream(zip.getBytes());
        try {
            var gzip = new GZIPInputStream(in);
            return gzip.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    ObjectMapper mapper = new ObjectMapper();
}
