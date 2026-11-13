package com.github.zeng.alt.message.rabbit;

import com.github.zeng.alt.message.MessageQueueTemplate;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * RabbitMQ 消息组件自动配置。
 * <p>
 * RabbitMQ message component auto-configuration.
 * <p>
 * 当 classpath 中存在 {@link RabbitTemplate} 时自动装配
 * {@link RabbitMessageQueueTemplate}。
 *
 * @author zengJiaJun
 * @since 2026-07-01
 * @version 1.0
 */
@AutoConfiguration
@ConditionalOnClass(RabbitTemplate.class)
public class RabbitMessageAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(MessageQueueTemplate.class)
    public MessageQueueTemplate rabbitMessageQueueTemplate(
            RabbitTemplate rabbitTemplate, AmqpAdmin amqpAdmin) {
        return new RabbitMessageQueueTemplate(rabbitTemplate, amqpAdmin);
    }
}
