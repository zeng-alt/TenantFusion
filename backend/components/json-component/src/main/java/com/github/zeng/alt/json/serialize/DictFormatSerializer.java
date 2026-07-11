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
import com.github.zeng.alt.json.spi.IDictEnum;
import com.github.zeng.alt.json.spi.IDictTranslateService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@JacksonStdImpl
public class DictFormatSerializer extends StdSerializer<Object> implements ContextualSerializer {

    private final String dictType;
    private final Map<String, String> enumLabelMap;

    public DictFormatSerializer() {
        super(Object.class);
        this.dictType = null;
        this.enumLabelMap = null;
    }

    public DictFormatSerializer(String dictType, Map<String, String> enumLabelMap) {
        super(Object.class);
        this.dictType = dictType;
        this.enumLabelMap = enumLabelMap;
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) {
        DictFormat ann = property != null
                ? property.getAnnotation(DictFormat.class)
                : null;
        if (ann == null) {
            return this;
        }
        Map<String, String> map = buildEnumMap(ann.enumClass());
        return new DictFormatSerializer(ann.dictType(), map);
    }

    private static Map<String, String> buildEnumMap(Class<? extends Enum<?>> enumClass) {
        if (enumClass == DictFormat.NoDictEnum.class) {
            return null;
        }
        Enum<?>[] constants = enumClass.getEnumConstants();
        if (constants == null || constants.length == 0) {
            return null;
        }
        Map<String, String> map = new HashMap<>(constants.length);
        for (Enum<?> constant : constants) {
            if (constant instanceof IDictEnum de) {
                String code = de.getCode();
                if (code == null) {
                    code = constant.name();
                }
                map.put(code, de.getLabel());
            } else {
                map.put(constant.name(), constant.name());
            }
        }
        return Map.copyOf(map);
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        String code = value.toString();

        if (enumLabelMap != null) {
            String label = enumLabelMap.get(code);
            if (label != null) {
                gen.writeString(label);
                return;
            }
            gen.writeString(code);
            return;
        }

        if (dictType != null && !dictType.isEmpty()) {
            IDictTranslateService service = DictServiceHolder.getService();
            if (service != null) {
                String label = service.translate(dictType, code);
                gen.writeString(Objects.requireNonNullElse(label, code));
                return;
            }
        }

        gen.writeString(code);
    }

    @Override
    public void serializeWithType(Object value, JsonGenerator gen, SerializerProvider provider,
                                  TypeSerializer typeSer) throws IOException {
        serialize(value, gen, provider);
    }
}
