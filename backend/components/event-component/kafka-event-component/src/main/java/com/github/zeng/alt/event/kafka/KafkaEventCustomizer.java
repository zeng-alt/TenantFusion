package com.github.zeng.alt.event.kafka;

import com.github.zeng.alt.event.spi.EventEndpointDto;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

/**
 * 贡献 Kafka 原生容器配置；确认模式由统一消费契约管理。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@FunctionalInterface
public interface KafkaEventCustomizer {
    void customize(EventEndpointDto endpoint, ConcurrentMessageListenerContainer<String, byte[]> container);
}
