package com.github.zeng.alt.message.kafka;

import com.github.zeng.alt.message.MessageQueueTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Kafka 消息组件自动配置。
 * <p>
 * Kafka message component auto-configuration.
 * <p>
 * 当 classpath 中存在 {@link KafkaTemplate} 时自动装配
 * {@link KafkaMessageQueueTemplate}。
 *
 * @author zengJiaJun
 * @since 2026-07-01
 * @version 1.0
 */
@AutoConfiguration
@ConditionalOnClass(KafkaTemplate.class)
public class KafkaMessageAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(MessageQueueTemplate.class)
    public MessageQueueTemplate kafkaMessageQueueTemplate(
            KafkaTemplate<String, byte[]> kafkaTemplate,
            ConsumerFactory<String, byte[]> consumerFactory,
            KafkaAdmin kafkaAdmin) {
        return new KafkaMessageQueueTemplate(kafkaTemplate, consumerFactory, kafkaAdmin);
    }
}
