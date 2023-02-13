package ikuyo.utils;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

public class NoCopyBox<T> {
    public T value;
    public NoCopyBox(T value) {
        this.value = value;
    }
    public static <T> NoCopyBox<T> of(T value) {
        return new NoCopyBox<>(value);
    }

    public static class Codec<T> implements MessageCodec<NoCopyBox<T>, NoCopyBox<T>> {
        @Override
        public void encodeToWire(Buffer buffer, NoCopyBox<T> tNoCopyBox) {
            throw new RuntimeException();
        }

        @Override
        public NoCopyBox<T> decodeFromWire(int pos, Buffer buffer) {
            throw new RuntimeException();
        }

        @Override
        public NoCopyBox<T> transform(NoCopyBox<T> tNoCopyBox) {
            return tNoCopyBox;
        }

        @Override
        public String name() {
            return "NoCopyBox";
        }

        @Override
        public byte systemCodecID() {
            return -1;
        }
    }
}
