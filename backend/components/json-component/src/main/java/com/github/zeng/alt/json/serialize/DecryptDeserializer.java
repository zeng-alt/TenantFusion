package com.github.zeng.alt.json.serialize;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.github.zeng.alt.json.spi.EncryptServiceHolder;
import com.github.zeng.alt.json.spi.IEncryptService;

import java.io.IOException;

/**
 * @author zengJiaJun
 * @since 2026年07月11日
 * @version 1.0
 */
@JacksonStdImpl
public class DecryptDeserializer extends StdDeserializer<String> {

    public static final DecryptDeserializer INSTANCE = new DecryptDeserializer();

    public DecryptDeserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        if (value == null || value.isEmpty()) {
            return value;
        }
        IEncryptService service = EncryptServiceHolder.getService();
        if (service == null) {
            return value;
        }
        return service.decrypt(value);
    }
}
