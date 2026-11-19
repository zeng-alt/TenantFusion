package com.github.zeng.alt.json.serialize;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.ser.std.StdSerializer;


/**
 * @author zengJiaJun
 * @since 2026年07月11日
 * @version 1.0
 */
@JacksonStdImpl
public class PhoneDesensitizeSerializer extends StdSerializer<String> {

    public static final PhoneDesensitizeSerializer INSTANCE = new PhoneDesensitizeSerializer();

    public PhoneDesensitizeSerializer() {
        super(String.class);
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializationContext provider) {
        if (value == null || value.isEmpty()) {
            gen.writeString(value);
            return;
        }
        String masked;
        if (value.length() >= 7) {
            masked = value.substring(0, 3) + "****" + value.substring(7);
        } else {
            char[] chars = value.toCharArray();
            for (int i = 1; i < chars.length; i++) {
                chars[i] = '*';
            }
            masked = new String(chars);
        }
        gen.writeString(masked);
    }
}
