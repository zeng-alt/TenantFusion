package com.github.zeng.alt.json.jackson3;

import java.util.function.Function;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.jsontype.TypeSerializer;

/**
 * 输出业务注解转换后的标量值。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
final class ConvertedValueSerializer extends ValueSerializer<Object> {
    private final Function<Object, Object> converter;

    ConvertedValueSerializer(Function<Object, Object> converter) {
        this.converter = converter;
    }

    @Override
    public void serialize(Object value, JsonGenerator generator, SerializationContext context) {
        final Object converted = converter.apply(value);
        if (converted instanceof String text) {
            generator.writeString(text);
            return;
        }
        if (converted instanceof Number number) {
            generator.writeNumber(number.toString());
            return;
        }
        context.writeValue(generator, converted);
    }

    @Override
    public void serializeWithType(
            Object value,
            JsonGenerator generator,
            SerializationContext context,
            TypeSerializer typeSerializer
    ) {
        serialize(value, generator, context);
    }
}
