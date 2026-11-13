package com.github.zeng.alt.event.internal;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zeng.alt.event.EventException;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.core.EventSerializer;

import java.util.Map;
import java.util.TreeMap;

/**
 * 为 Modulith 序列化记录附加上下文；原事件类型与原序列化器保持不变。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor
public class ContextEventSerializer implements EventSerializer {

    private static final String FORMAT = "$alt.event.context";
    private static final TypeReference<Map<String, String>> CONTEXT_TYPE = new TypeReference<>() {};

    private final EventSerializer delegate;
    private final EventContextHelper snapshots;
    private final ObjectMapper envelopeMapper = new ObjectMapper();

    @Override
    public Object serialize(Object event) {
        final var envelope = envelopeMapper.createObjectNode();
        envelope.put(FORMAT, 1);
        envelope.set("context", envelopeMapper.valueToTree(new TreeMap<>(snapshots.capture(event))));
        envelope.put("serialized", delegate.serialize(event).toString());
        return Try.of(() -> envelopeMapper.writeValueAsString(envelope))
                .getOrElseThrow(failure -> new EventException("事件上下文序列化失败", failure));
    }

    @Override
    public <T> T deserialize(Object serialized, Class<T> type) {
        return Try.of(() -> envelopeMapper.readTree(serialized.toString())).toOption()
                .filter(node -> node.path(FORMAT).asInt() == 1)
                .map(envelope -> deserializeEnvelope(envelope, type))
                .getOrElse(() -> {
                    final T event = delegate.deserialize(serialized, type);
                    snapshots.remember(event, Map.of());
                    return event;
                });
    }

    private <T> T deserializeEnvelope(JsonNode envelope, Class<T> type) {
        final Map<String, String> contextByKey = envelopeMapper.convertValue(envelope.required("context"), CONTEXT_TYPE);
        final T event = delegate.deserialize(envelope.required("serialized").asText(), type);
        snapshots.remember(event, contextByKey);
        return event;
    }
}
