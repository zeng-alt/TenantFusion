package com.github.zeng.alt.json.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.github.zeng.alt.json.spi.EncryptServiceHolder;
import com.github.zeng.alt.json.spi.IEncryptService;

import java.io.IOException;

@JacksonStdImpl
public class EncryptSerializer extends StdSerializer<String> {

    public static final EncryptSerializer INSTANCE = new EncryptSerializer();

    public EncryptSerializer() {
        super(String.class);
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null || value.isEmpty()) {
            gen.writeString(value);
            return;
        }
        IEncryptService service = EncryptServiceHolder.getService();
        if (service == null) {
            gen.writeString(value);
            return;
        }
        gen.writeString(service.encrypt(value));
    }

    @Override
    public void serializeWithType(String value, JsonGenerator gen, SerializerProvider provider,
                                  TypeSerializer typeSer) throws IOException {
        serialize(value, gen, provider);
    }
}
