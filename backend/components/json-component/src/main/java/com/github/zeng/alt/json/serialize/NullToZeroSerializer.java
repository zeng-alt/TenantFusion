package com.github.zeng.alt.json.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

@JacksonStdImpl
public class NullToZeroSerializer extends StdSerializer<Object> {

    public static final NullToZeroSerializer INSTANCE = new NullToZeroSerializer();

    public NullToZeroSerializer() {
        super(Object.class);
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null) {
            gen.writeNumber(0);
        } else if (value instanceof Number n) {
            gen.writeNumber(n.toString());
        } else {
            gen.writeObject(value);
        }
    }

    @Override
    public void serializeWithType(Object value, JsonGenerator gen, SerializerProvider provider,
                                  TypeSerializer typeSer) throws IOException {
        serialize(value, gen, provider);
    }
}
