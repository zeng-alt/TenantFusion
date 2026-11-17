package com.github.zeng.alt.json.jackson3;

import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.collection.CharSeq;
import io.vavr.collection.Map;
import io.vavr.control.Either;
import io.vavr.control.Option;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/**
 * 将 Vavr 值输出为普通 JSON 标量、数组或对象。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
final class VavrValueSerializer extends ValueSerializer<Object> {
    @Override
    public void serialize(Object value, JsonGenerator generator, SerializationContext context) {
        if (value instanceof Option<?> option) {
            context.writeValue(generator, option.getOrNull());
            return;
        }
        if (value instanceof Lazy<?> lazy) {
            context.writeValue(generator, lazy.get());
            return;
        }
        if (value instanceof CharSeq characters) {
            generator.writeString(characters.toString());
            return;
        }
        if (value instanceof Map<?, ?> map) {
            context.writeValue(generator, map.toJavaMap());
            return;
        }
        if (value instanceof Either<?, ?> either) {
            generator.writeStartArray();
            generator.writeString(either.isLeft() ? "left" : "right");
            context.writeValue(generator, either.isLeft() ? either.getLeft() : either.get());
            generator.writeEndArray();
            return;
        }
        final Iterable<?> elements = value instanceof Tuple tuple ? tuple.toSeq() : (Iterable<?>) value;
        generator.writeStartArray();
        for (Object element : elements) {
            context.writeValue(generator, element);
        }
        generator.writeEndArray();
    }

    @Override
    public boolean isEmpty(SerializationContext context, Object value) {
        return value == null || value instanceof Option<?> option && option.isEmpty();
    }
}
