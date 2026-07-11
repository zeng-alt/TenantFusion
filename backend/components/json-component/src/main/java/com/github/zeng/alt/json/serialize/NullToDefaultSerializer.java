package com.github.zeng.alt.json.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.github.zeng.alt.json.annotation.NullToDefault;

import java.io.IOException;

/**
 * @author zengJiaJun
 * @since 2026年07月11日
 * @version 1.0
 */
@JacksonStdImpl
public class NullToDefaultSerializer extends StdSerializer<String> implements ContextualSerializer {

    private final String defaultValue;

    public NullToDefaultSerializer() {
        super(String.class);
        this.defaultValue = null;
    }

    public NullToDefaultSerializer(String defaultValue) {
        super(String.class);
        this.defaultValue = defaultValue;
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) {
        NullToDefault ann = property != null
                ? property.getAnnotation(NullToDefault.class)
                : null;
        if (ann != null) {
            return new NullToDefaultSerializer(ann.value());
        }
        return this;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(value != null ? value : defaultValue);
    }

    @Override
    public void serializeWithType(String value, JsonGenerator gen, SerializerProvider provider,
                                  TypeSerializer typeSer) throws IOException {
        serialize(value, gen, provider);
    }
}
