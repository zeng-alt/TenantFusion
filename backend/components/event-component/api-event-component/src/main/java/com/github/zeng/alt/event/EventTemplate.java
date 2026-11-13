package com.github.zeng.alt.event;

import com.github.zeng.alt.event.config.EventBindingProperties;
import com.github.zeng.alt.event.context.EventContextTemplate;
import com.github.zeng.alt.event.spi.EventMessageDto;
import com.github.zeng.alt.event.spi.EventMetadataDto;
import com.github.zeng.alt.event.support.EventCodec;
import com.github.zeng.alt.event.support.EventTransportResolver;

import io.vavr.control.Option;

import lombok.RequiredArgsConstructor;

import org.springframework.util.Assert;

import java.util.LinkedHashMap;
import java.util.Map;

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

    /** 创建单次发送构建器，绑定名对应 alt.event.bindings 配置。 */
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
        private final Map<String, String> headerByName = new LinkedHashMap<>();

        private EventPublishBuilder(String binding) {
            this.binding = binding;
        }

        public EventPublishBuilder header(String name, String value) {
            headerByName.put(name, value);
            return this;
        }

        /** 指定稳定事件 ID，供 Outbox 重发和业务幂等使用；不自动去重。 */
        public EventPublishBuilder messageId(String id) {
            Assert.hasText(id, "event messageId must not be blank");
            messageId = Option.of(id);
            return this;
        }

        /** 设置 Kafka 分区键；其他传输将该值保留为 event.key header。 */
        public EventPublishBuilder key(String key) {
            return header("event.key", key);
        }

        /** 捕获当前线程上下文并同步发送，返回事件 ID；传输失败抛出 EventException。 */
        public String publish(Object payload) {
            final EventBindingProperties configuration = transports.binding(binding);
            final EventMetadataDto captured = EventMetadataDto.create(contexts.capture(), headerByName);
            final EventMetadataDto metadata = messageId
                    .map(id -> new EventMetadataDto(id, captured.context(), captured.headers(), captured.retry()))
                    .getOrElse(captured);
            transports.transport(configuration.getTransport())
                    .publish(configuration, new EventMessageDto(metadata, codec.encode(payload)));
            return metadata.id();
        }
    }
}
