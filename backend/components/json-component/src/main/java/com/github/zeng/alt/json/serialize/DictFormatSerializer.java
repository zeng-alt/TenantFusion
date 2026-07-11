package com.github.zeng.alt.json.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.github.zeng.alt.json.annotation.DictFormat;
import com.github.zeng.alt.json.spi.DictServiceHolder;
import com.github.zeng.alt.json.spi.IDictTranslateService;

import java.io.IOException;
import java.util.Objects;

@JacksonStdImpl
public class DictFormatSerializer extends StdSerializer<Object> implements ContextualSerializer {

    private final String dictType;

    public DictFormatSerializer() {
        super(Object.class);
        this.dictType = null;
    }

    public DictFormatSerializer(String dictType) {
        super(Object.class);
        this.dictType = dictType;
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) {
        DictFormat ann = property != null
                ? property.getAnnotation(DictFormat.class)
                : null;
        if (ann != null) {
            return new DictFormatSerializer(ann.dictType());
        }
        return this;
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        IDictTranslateService service = DictServiceHolder.getService();
        if (service == null) {
            gen.writeString(value.toString());
            return;
        }
        String code = value.toString();
        String label = service.translate(dictType, code);
        gen.writeString(Objects.requireNonNullElse(label, code));
    }

    @Override
    public void serializeWithType(Object value, JsonGenerator gen, SerializerProvider provider,
                                  TypeSerializer typeSer) throws IOException {
        serialize(value, gen, provider);
    }
}
