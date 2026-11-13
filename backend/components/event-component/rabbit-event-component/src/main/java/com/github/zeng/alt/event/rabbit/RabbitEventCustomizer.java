package com.github.zeng.alt.event.rabbit;

import com.github.zeng.alt.event.spi.EventEndpointDto;

import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;

/**
 * 贡献 Rabbit 原生容器选项；消费确认由统一结果契约管理。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@FunctionalInterface
public interface RabbitEventCustomizer {
    void customize(EventEndpointDto endpoint, SimpleMessageListenerContainer container);
}
