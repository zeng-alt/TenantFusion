package com.github.zeng.alt.json.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

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
    public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
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
