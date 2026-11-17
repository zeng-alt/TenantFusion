package com.github.zeng.alt.json.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.github.zeng.alt.json.annotation.Sensitive;
import com.github.zeng.alt.json.annotation.SensitiveType;
import com.github.zeng.alt.json.internal.SensitiveValueHelper;

import java.io.IOException;

/**
 * @author zengJiaJun
 * @since 2026年07月11日
 * @version 1.0
 */
@JacksonStdImpl
public class SensitiveSerializer extends StdSerializer<String> implements ContextualSerializer {

    private final SensitiveType type;
    private final String placeholder;

    public SensitiveSerializer() {
        super(String.class);
        this.type = null;
        this.placeholder = null;
    }

    public SensitiveSerializer(SensitiveType type, String placeholder) {
        super(String.class);
        this.type = type;
        this.placeholder = placeholder;
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) {
        Sensitive ann = property != null
                ? property.getAnnotation(Sensitive.class)
                : null;
        if (ann != null) {
            return new SensitiveSerializer(ann.type(), ann.placeholder());
        }
        return this;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null || value.isEmpty()) {
            gen.writeString(value);
            return;
        }
        gen.writeString(mask(value));
    }

    @Override
    public void serializeWithType(String value, JsonGenerator gen, SerializerProvider provider,
                                  TypeSerializer typeSer) throws IOException {
        serialize(value, gen, provider);
    }

    private String mask(String value) {
        return SensitiveValueHelper.mask(value, type, placeholder);
    }
}
