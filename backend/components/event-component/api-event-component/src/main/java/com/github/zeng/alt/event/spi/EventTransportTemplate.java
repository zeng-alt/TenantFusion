package com.github.zeng.alt.event.spi;

import com.github.zeng.alt.event.config.EventBindingProperties;

/**
 * 中间件 SPI；支持多个实现同时装配，不依赖全局唯一的消息模板。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public interface EventTransportTemplate {
    String transport();
    void validate(EventEndpointDto endpoint);
    EventSubscriptionHandler subscribe(EventEndpointDto endpoint, EventConsumer consumer);
    void publish(EventBindingProperties binding, EventMessageDto message);
}
