package com.github.zeng.alt.json.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

@JacksonStdImpl
public class PhoneDesensitizeSerializer extends StdSerializer<String> {

    public static final PhoneDesensitizeSerializer INSTANCE = new PhoneDesensitizeSerializer();

    public PhoneDesensitizeSerializer() {
        super(String.class);
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
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
