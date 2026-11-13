package com.github.zeng.alt.event.kafka;

import com.github.zeng.alt.event.EventTemplate;
import com.github.zeng.alt.event.support.EventCodec;
import com.github.zeng.alt.event.spi.EventMetadataDto;
import com.github.zeng.alt.event.testing.EventBrokerTestSupport;
import com.github.zeng.alt.event.testing.EventBrokerTestSupport.Listener;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import java.time.Duration;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaKraftBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import java.util.List;

/**
 * 真实 Broker 上的统一监听、上下文、重试和死信验证。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class KafkaEventTemplateTest {
    private static final EmbeddedKafkaKraftBroker BROKER = new EmbeddedKafkaKraftBroker(1, 1);
    @BeforeAll static void startBroker() { BROKER.afterPropertiesSet(); }
    @AfterAll static void stopBroker() { BROKER.destroy(); }

    @Test
    void retriesWithOriginalContextAndStableMessageId() {
        final var listener = new Listener();
        runner("retry-" + UUID.randomUUID(), listener).run(context -> {
            assertThat(context).hasNotFailed();
            final String id = EventBrokerTestSupport.publish(context.getBean(EventTemplate.class), "retry");
            EventBrokerTestSupport.assertAttempts(listener, id, 2);
        });
    }

    @Test
    void exhaustsBudgetAndPublishesDeadLetter() {
        final String topic = "budget-" + UUID.randomUUID();
        final var listener = new Listener();
        runner(topic, listener).run(context -> {
            assertThat(context).hasNotFailed();
            final String id = EventBrokerTestSupport.publish(context.getBean(EventTemplate.class), "fail");
            EventBrokerTestSupport.assertAttempts(listener, id, 3);
            try (var reader = consumers("dead-reader-" + UUID.randomUUID()).createConsumer()) {
                reader.subscribe(List.of(topic + ".dead"));
                final var dead = KafkaTestUtils.getSingleRecord(reader, topic + ".dead", Duration.ofSeconds(20));
                final var metadata = context.getBean(EventCodec.class)
                        .metadata(dead.headers().lastHeader(EventCodec.METADATA_HEADER).value());
                assertThat(metadata.id()).isEqualTo(id);
                assertThat(metadata.retry().count()).isEqualTo(2);
                assertThat(dead.key()).isEqualTo("order-1");
            }
        });
    }

    private ApplicationContextRunner runner(String topic, Listener listener) {
        BROKER.addTopics(topic, topic + ".retry", topic + ".dead");
        final var properties = KafkaTestUtils.producerProps(BROKER);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        final var factory = new DefaultKafkaProducerFactory<String, byte[]>(properties);
        final var publisher = new KafkaTemplate<>(factory);
        return EventBrokerTestSupport.runner(listener)
                .withConfiguration(AutoConfigurations.of(KafkaEventAutoConfiguration.class))
                .withBean(DefaultKafkaProducerFactory.class, () -> factory)
                .withBean(KafkaTemplate.class, () -> publisher)
                .withBean(KafkaEventCustomizer.class, () -> (endpoint, container) ->
                        container.getContainerProperties().setPollTimeout(50))
                .withBean(DefaultKafkaConsumerFactory.class, () -> consumers("orders"))
                .withPropertyValues("alt.event.bindings.test.transport=kafka",
                        "alt.event.bindings.test.kafka.topic=" + topic,
                        "alt.event.bindings.test.kafka.group-id=orders",
                        "alt.event.bindings.test.kafka.retry-topic=" + topic + ".retry",
                        "alt.event.bindings.test.failure.destination=" + topic + ".dead");
    }

    private DefaultKafkaConsumerFactory<String, byte[]> consumers(String group) {
        final var properties = KafkaTestUtils.consumerProps(group, "false", BROKER);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(properties);
    }
}
