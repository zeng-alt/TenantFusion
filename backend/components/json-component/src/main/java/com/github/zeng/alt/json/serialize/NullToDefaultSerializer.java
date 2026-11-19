package com.github.zeng.alt.json.serialize;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;
import com.github.zeng.alt.json.annotation.NullToDefault;


/**
 * @author zengJiaJun
 * @since 2026年07月11日
 * @version 1.0
 */
@JacksonStdImpl
public class NullToDefaultSerializer extends StdSerializer<String> {

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
    public ValueSerializer<?> createContextual(SerializationContext prov, BeanProperty property) {
        NullToDefault ann = property != null
                ? property.getAnnotation(NullToDefault.class)
                : null;
        if (ann != null) {
            return new NullToDefaultSerializer(ann.value());
        }
        return this;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializationContext provider) {
        gen.writeString(value != null ? value : defaultValue);
    }

    @Override
    public void serializeWithType(String value, JsonGenerator gen, SerializationContext provider,
                                  TypeSerializer typeSer) {
        serialize(value, gen, provider);
    }
}
