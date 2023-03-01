package ikuyo.utils;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

public class RawDeserializer extends StdDeserializer<String> {
    protected RawDeserializer(Class<?> vc) {
        super(vc);
    }

    public RawDeserializer() {
        super((Class<?>) null);
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        return ctxt.readTree(p).toString();
    }
}
