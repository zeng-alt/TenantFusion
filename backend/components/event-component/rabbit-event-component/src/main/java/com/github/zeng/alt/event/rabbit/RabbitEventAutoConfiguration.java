package com.github.zeng.alt.event.rabbit;

import com.github.zeng.alt.event.config.EventAutoConfiguration;
import com.github.zeng.alt.event.support.EventCodec;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 装配 Rabbit 原生传输，保留宿主的 RabbitTemplate 和连接配置。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@AutoConfiguration(after = EventAutoConfiguration.class, afterName = "org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration")
public class RabbitEventAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(RabbitEventTemplate.class)
    public RabbitEventTemplate rabbitEventTemplate(
            RabbitTemplate publisher,
            AmqpAdmin admin,
            EventCodec codec,
            ObjectProvider<RabbitEventCustomizer> customizers
    ) {
        return new RabbitEventTemplate(publisher, admin, codec, customizers);
    }
}
