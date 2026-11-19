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
public class NullToEmptySerializer extends StdSerializer<String> {

    public static final NullToEmptySerializer INSTANCE = new NullToEmptySerializer();

    public NullToEmptySerializer() {
        super(String.class);
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializationContext provider) {
        gen.writeString(value != null ? value : "");
    }

    @Override
    public void serializeWithType(String value, JsonGenerator gen, SerializationContext provider,
                                  TypeSerializer typeSer) {
        serialize(value, gen, provider);
    }
}
