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
public class NameDesensitizeSerializer extends StdSerializer<String> {

    public static final NameDesensitizeSerializer INSTANCE = new NameDesensitizeSerializer();

    public NameDesensitizeSerializer() {
        super(String.class);
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializationContext provider) {
        if (value == null || value.isEmpty()) {
            gen.writeString(value);
            return;
        }
        char[] chars = value.toCharArray();
        for (int i = 1; i < chars.length; i++) {
            chars[i] = '*';
        }
        gen.writeString(new String(chars));
    }
}
