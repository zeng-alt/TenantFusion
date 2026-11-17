package com.github.zeng.alt.json.jackson3;

import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.collection.CharSeq;
import io.vavr.collection.Map;
import io.vavr.control.Either;
import io.vavr.control.Option;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;

/**
 * 按声明的泛型类型重建 Vavr 值。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
final class VavrValueDeserializer extends ValueDeserializer<Object> {
    private final JavaType type;

    VavrValueDeserializer(JavaType type) {
        this.type = type;
    }

    @Override
    public Object deserialize(JsonParser parser, DeserializationContext context) {
        final JsonNode node = context.readTree(parser);
        final Class<?> raw = type.getRawClass();
        if (Option.class.isAssignableFrom(raw)) {
            return Option.of(context.readTreeAsValue(node, type.containedTypeOrUnknown(0)));
        }
        if (Lazy.class.isAssignableFrom(raw)) {
            final Object value = context.readTreeAsValue(node, type.containedTypeOrUnknown(0));
            return Lazy.of(() -> value);
        }
        if (Either.class.isAssignableFrom(raw)) {
            return either(node, context);
        }
        if (Map.class.isAssignableFrom(raw)) {
            final JavaType mapType = context.getTypeFactory().constructMapType(
                    LinkedHashMap.class,
                    type.containedTypeOrUnknown(0),
                    type.containedTypeOrUnknown(1)
            );
            return VavrCollectionHelper.map(raw, context.readTreeAsValue(node, mapType));
        }
        if (CharSeq.class.isAssignableFrom(raw) && node.isString()) {
            return CharSeq.of(node.stringValue());
        }
        if (!node.isArray()) {
            return context.reportInputMismatch(type, "Expected JSON array for %s", raw.getName());
        }
        if (Tuple.class.isAssignableFrom(raw)) {
            return tuple(node, context);
        }
        final List<Object> elements = new ArrayList<>(node.size());
        node.forEach(element -> elements.add(context.readTreeAsValue(element, type.containedTypeOrUnknown(0))));
        return VavrCollectionHelper.collection(raw, elements);
    }

    @Override
    public Object getNullValue(DeserializationContext context) {
        return Option.class.isAssignableFrom(type.getRawClass()) ? Option.none() : null;
    }

    private Object either(JsonNode node, DeserializationContext context) {
        final String side;
        final JsonNode value;
        if (node.isArray() && node.size() == 2 && node.get(0).isString()) {
            side = node.get(0).stringValue();
            value = node.get(1);
        } else if (node.isObject() && node.size() == 1) {
            final java.util.Map.Entry<String, JsonNode> entry = node.properties().iterator().next();
            side = entry.getKey();
            value = entry.getValue();
        } else {
            return context.reportInputMismatch(type, "Expected [left|right, value] for Either");
        }
        return switch (side) {
            case "left", "l" -> Either.left(context.readTreeAsValue(value, type.containedTypeOrUnknown(0)));
            case "right", "r" -> Either.right(context.readTreeAsValue(value, type.containedTypeOrUnknown(1)));
            default -> context.reportInputMismatch(type, "Unknown Either branch: %s", side);
        };
    }

    private Object tuple(JsonNode node, DeserializationContext context) {
        final int arity = type.containedTypeCount();
        if (node.size() != arity) {
            return context.reportInputMismatch(type, "Expected tuple of size %d", arity);
        }
        final Object[] elements = new Object[arity];
        for (int i = 0; i < arity; i++) {
            elements[i] = context.readTreeAsValue(node.get(i), type.containedTypeOrUnknown(i));
        }
        return switch (arity) {
            case 0 -> Tuple.empty();
            case 1 -> Tuple.of(elements[0]);
            case 2 -> Tuple.of(elements[0], elements[1]);
            case 3 -> Tuple.of(elements[0], elements[1], elements[2]);
            case 4 -> Tuple.of(elements[0], elements[1], elements[2], elements[3]);
            case 5 -> Tuple.of(elements[0], elements[1], elements[2], elements[3], elements[4]);
            case 6 -> Tuple.of(elements[0], elements[1], elements[2], elements[3], elements[4], elements[5]);
            case 7 -> Tuple.of(
                    elements[0],
                    elements[1],
                    elements[2],
                    elements[3],
                    elements[4],
                    elements[5],
                    elements[6]
            );
            case 8 -> Tuple.of(
                    elements[0],
                    elements[1],
                    elements[2],
                    elements[3],
                    elements[4],
                    elements[5],
                    elements[6],
                    elements[7]
            );
            default -> context.reportInputMismatch(type, "Unsupported tuple arity: %d", arity);
        };
    }
}
