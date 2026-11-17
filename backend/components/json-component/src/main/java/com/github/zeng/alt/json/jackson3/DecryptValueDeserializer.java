package com.github.zeng.alt.json.jackson3;

import com.github.zeng.alt.json.spi.EncryptServiceHolder;
import io.vavr.control.Option;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/**
 * Jackson 3 字符串解密适配。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
final class DecryptValueDeserializer extends ValueDeserializer<String> {
    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) {
        final String value = parser.getValueAsString();
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Option.of(EncryptServiceHolder.getService()).map(service -> service.decrypt(value)).getOrElse(value);
    }
}
