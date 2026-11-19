package com.github.zeng.alt.json.serialize;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;
import com.github.zeng.alt.json.annotation.Sensitive;
import com.github.zeng.alt.json.annotation.SensitiveType;
import com.github.zeng.alt.json.internal.SensitiveValueHelper;


/**
 * @author zengJiaJun
 * @since 2026年07月11日
 * @version 1.0
 */
@JacksonStdImpl
public class SensitiveSerializer extends StdSerializer<String> {

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
    public ValueSerializer<?> createContextual(SerializationContext prov, BeanProperty property) {
        Sensitive ann = property != null
                ? property.getAnnotation(Sensitive.class)
                : null;
        if (ann != null) {
            return new SensitiveSerializer(ann.type(), ann.placeholder());
        }
        return this;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializationContext provider) {
        if (value == null || value.isEmpty()) {
            gen.writeString(value);
            return;
        }
        gen.writeString(mask(value));
    }

    @Override
    public void serializeWithType(String value, JsonGenerator gen, SerializationContext provider,
                                  TypeSerializer typeSer) {
        serialize(value, gen, provider);
    }

    private String mask(String value) {
        return SensitiveValueHelper.mask(value, type, placeholder);
    }
}
