package com.github.zeng.alt.event;

import com.github.zeng.alt.event.context.EventContextTemplate;
import com.github.zeng.alt.event.spi.EventMessageDto;
import com.github.zeng.alt.event.spi.EventMetadataDto;
import com.github.zeng.alt.event.support.EventCodec;
import com.github.zeng.alt.event.support.EventTransportResolver;
import lombok.RequiredArgsConstructor;
import java.util.LinkedHashMap;
import java.util.Map;
import io.vavr.control.Option;
import org.springframework.util.Assert;

/**
 * 按逻辑绑定发送事件的流式门面；保留业务负载类型并捕获来源上下文。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor
public class EventTemplate {
    private final EventTransportResolver transports;
    private final EventContextTemplate contexts;
    private final EventCodec codec;

    public EventPublishBuilder to(String binding) {
        return new EventPublishBuilder(binding);
    }
    /**
     * 一次发送的构建器；上下文在 publish 时捕获。
     *
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    public final class EventPublishBuilder {
        private final String binding;
        private Option<String> messageId = Option.none();
        private final Map<String, String> headers = new LinkedHashMap<>();
        private EventPublishBuilder(String binding) { this.binding = binding; }
        public EventPublishBuilder header(String name, String value) {
            headers.put(name, value);
            return this;
        }
        public EventPublishBuilder messageId(String id) {
            Assert.hasText(id, "event messageId must not be blank");
            messageId = Option.of(id);
            return this;
        }
        public EventPublishBuilder key(String key) { return header("event.key", key); }
        public String publish(Object payload) {
            final var configuration = transports.binding(binding);
            final var captured = EventMetadataDto.create(contexts.capture(), headers);
            final var metadata = messageId.map(id -> new EventMetadataDto(
                    id, captured.context(), captured.headers(), captured.retry())).getOrElse(captured);
            transports.transport(configuration.getTransport())
                    .publish(configuration, new EventMessageDto(metadata, codec.encode(payload)));
            return metadata.id();
        }
    }
}
