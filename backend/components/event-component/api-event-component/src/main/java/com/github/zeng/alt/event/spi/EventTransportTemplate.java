package com.github.zeng.alt.event.spi;

import com.github.zeng.alt.event.config.EventBindingProperties;

/**
 * 中间件 SPI；支持多个实现同时装配，不依赖全局唯一的消息模板。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public interface EventTransportTemplate {
    /** 返回绑定配置使用的传输标识，多个实现的标识不可重复。 */
    String transport();

    /** 在创建订阅前校验当前传输能够兑现端点配置的消费语义。 */
    void validate(EventEndpointDto endpoint);

    /** 创建尚未启动的订阅句柄，由统一注册中心管理生命周期。 */
    EventSubscriptionHandler subscribe(EventEndpointDto endpoint, EventConsumer consumer);

    /** 同步发送信封；可靠传输返回前应完成 Broker 确认，Pub/Sub 仅提供在线通知。 */
    void publish(EventBindingProperties binding, EventMessageDto message);
}
