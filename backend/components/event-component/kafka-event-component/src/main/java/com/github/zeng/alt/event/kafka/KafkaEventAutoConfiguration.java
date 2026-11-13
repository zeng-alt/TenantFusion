package com.github.zeng.alt.event.kafka;

import com.github.zeng.alt.event.config.EventAutoConfiguration;
import com.github.zeng.alt.event.support.EventCodec;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * 装配 Kafka 传输，允许与 Rabbit、Redis 同时使用。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@AutoConfiguration(after = EventAutoConfiguration.class, afterName = "org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration")
public class KafkaEventAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(KafkaEventTemplate.class)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public KafkaEventTemplate kafkaEventTemplate(
            KafkaTemplate<?, ?> publisher,
            ConsumerFactory<?, ?> consumers,
            EventCodec codec,
            ObjectProvider<KafkaEventCustomizer> customizers
    ) {
        return new KafkaEventTemplate((KafkaTemplate) publisher, (ConsumerFactory) consumers, codec, customizers);
    }
}
