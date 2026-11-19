package com.github.zeng.alt.json.serialize;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;
import com.github.zeng.alt.json.spi.EncryptServiceHolder;
import com.github.zeng.alt.json.spi.IEncryptService;


/**
 * @author zengJiaJun
 * @since 2026年07月11日
 * @version 1.0
 */
@JacksonStdImpl
public class EncryptSerializer extends StdSerializer<String> {

    public static final EncryptSerializer INSTANCE = new EncryptSerializer();

    public EncryptSerializer() {
        super(String.class);
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializationContext provider) {
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
    public void serializeWithType(String value, JsonGenerator gen, SerializationContext provider,
                                  TypeSerializer typeSer) {
        serialize(value, gen, provider);
    }
}
