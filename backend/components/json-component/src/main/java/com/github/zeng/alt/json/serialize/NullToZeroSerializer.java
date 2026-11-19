package com.github.zeng.alt.json.serialize;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;


/**
 * @author zengJiaJun
 * @since 2026年07月11日
 * @version 1.0
 */
@JacksonStdImpl
public class NullToZeroSerializer extends StdSerializer<Object> {

    public static final NullToZeroSerializer INSTANCE = new NullToZeroSerializer();

    public NullToZeroSerializer() {
        super(Object.class);
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializationContext provider) {
        if (value == null) {
            gen.writeNumber(0);
        } else if (value instanceof Number n) {
            gen.writeNumber(n.toString());
        } else {
            provider.writeValue(gen, value);
        }
    }

    @Override
    public void serializeWithType(Object value, JsonGenerator gen, SerializationContext provider,
                                  TypeSerializer typeSer) {
        serialize(value, gen, provider);
    }
}
