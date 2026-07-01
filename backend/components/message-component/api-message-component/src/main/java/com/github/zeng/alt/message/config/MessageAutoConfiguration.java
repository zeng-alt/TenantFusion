package com.github.zeng.alt.message.config;

import com.github.zeng.alt.message.MessageQueueTemplate;
import com.github.zeng.alt.message.MessageRuntimeHints;
import com.github.zeng.alt.message.codec.JacksonMessagePacketCodec;
import com.github.zeng.alt.message.codec.MessagePacketCodec;
import com.github.zeng.alt.message.subscription.MessageListenerBeanPostProcessor;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * 消息模块自动配置。
 * <p>
 * Auto-configuration for the message module.
 * <p>
 * 提供默认的 {@link MessagePacketCodec} 和 {@link MessageListenerBeanPostProcessor} 等公共 Bean。
 * 各实现模块（Redis / RabbitMQ / Kafka）的自动配置需要在此配置之后加载。
 *
 * @author zengJiaJun
 * @since 2026-07-01
 * @version 1.0
 */
@Configuration
@ImportRuntimeHints(MessageRuntimeHints.class)
public class MessageAutoConfiguration {

    /**
     * 默认的消息包编解码器（Jackson JSON 实现）。
     */
    @Bean
    @ConditionalOnMissingBean
    public MessagePacketCodec messagePacketCodec() {
        return new JacksonMessagePacketCodec();
    }

    /**
     * {@code @MessageListener} 注解的消息订阅注册器。
     * 仅在存在 {@link MessageQueueTemplate} 时生效，
     * 由实现模块（Redis / RabbitMQ / Kafka）提供。
     */
    @Bean
    @ConditionalOnBean(MessageQueueTemplate.class)
    @ConditionalOnMissingBean
    public MessageListenerBeanPostProcessor messageListenerBeanPostProcessor(
            ListableBeanFactory beanFactory,
            MessageQueueTemplate messageQueueTemplate) {
        return new MessageListenerBeanPostProcessor(beanFactory, messageQueueTemplate);
    }
}
