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

import java.io.IOException;

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
        String p = placeholder;
        return switch (type) {
            case PHONE -> maskPhone(value, p);
            case NAME -> maskName(value, p);
            case EMAIL -> maskEmail(value, p);
            case ID_CARD -> maskIdCard(value, p);
            case BANK_CARD -> maskBankCard(value, p);
            case ADDRESS -> maskAddress(value, p);
            case PASSWORD -> maskPassword(p);
        };
    }

    private static String maskPhone(String value, String p) {
        if (value.length() < 7) {
            return value.charAt(0) + repeat(p, value.length() - 1);
        }
        return value.substring(0, 3) + repeat(p, 4) + value.substring(7);
    }

    private static String maskName(String value, String p) {
        return value.charAt(0) + repeat(p, value.length() - 1);
    }

    private static String maskEmail(String value, String p) {
        int at = value.indexOf('@');
        if (at <= 0) {
            return value;
        }
        return value.charAt(0) + repeat(p, Math.min(3, at - 1)) + value.substring(at);
    }

    private static String maskIdCard(String value, String p) {
        int len = value.length();
        if (len < 8) {
            return value;
        }
        return value.substring(0, 3) + repeat(p, len - 7) + value.substring(len - 4);
    }

    private static String maskBankCard(String value, String p) {
        int len = value.length();
        if (len < 8) {
            return value;
        }
        return value.substring(0, 4) + repeat(p, len - 8) + value.substring(len - 4);
    }

    private static String maskAddress(String value, String p) {
        int keep = Math.min(6, value.length());
        return value.substring(0, keep) + repeat(p, Math.min(4, value.length() - keep));
    }

    private static String maskPassword(String p) {
        return repeat(p, 6);
    }

    private static String repeat(String s, int count) {
        if (count <= 0) return "";
        return s.repeat(count);
    }
}
