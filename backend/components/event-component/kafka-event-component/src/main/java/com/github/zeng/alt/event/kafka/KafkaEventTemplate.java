package com.github.zeng.alt.event.kafka;

import com.github.zeng.alt.event.EventException;
import com.github.zeng.alt.event.EventOutcome;
import com.github.zeng.alt.event.config.EventBindingProperties;
import com.github.zeng.alt.event.spi.EventConsumer;
import com.github.zeng.alt.event.spi.EventEndpointDto;
import com.github.zeng.alt.event.spi.EventMessageDto;
import com.github.zeng.alt.event.spi.EventMetadataDto;
import com.github.zeng.alt.event.spi.EventSubscriptionHandler;
import com.github.zeng.alt.event.spi.EventTransportTemplate;
import com.github.zeng.alt.event.support.EventCodec;
import com.github.zeng.alt.event.support.EventSubscriptionHandlerImpl;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.util.Assert;
import org.springframework.util.backoff.FixedBackOff;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Kafka 消费与持久化重试适配；重试 topic 转发确认后才提交源 offset。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor
public class KafkaEventTemplate implements EventTransportTemplate {
    private final KafkaTemplate<String, byte[]> publisher;
    private final ConsumerFactory<String, byte[]> consumers;
    private final EventCodec codec;
    private final ObjectProvider<KafkaEventCustomizer> customizers;

    @Override public String transport() { return "kafka"; }

    @Override
    public void validate(EventEndpointDto endpoint) {
        final var kafka = endpoint.binding().getKafka();
        Assert.hasText(kafka.getTopic(), "kafka.topic is required");
        Assert.hasText(kafka.getGroupId(), "kafka.group-id is required");
        Assert.hasText(kafka.getRetryTopic(), "kafka.retry-topic is required");
        Assert.isTrue(!kafka.getTopic().equals(kafka.getRetryTopic()), "retry topic must be separate from source topic");
        Assert.isTrue(kafka.getConcurrency() > 0, "kafka.concurrency must be positive");
    }

    @Override
    public EventSubscriptionHandler subscribe(EventEndpointDto endpoint, EventConsumer consumer) {
        final var kafka = endpoint.binding().getKafka();
        final var properties = new ContainerProperties(kafka.getTopic(), kafka.getRetryTopic());
        properties.setGroupId(kafka.getGroupId());
        properties.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        final var overrides = new Properties();
        overrides.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        overrides.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        overrides.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        properties.setKafkaConsumerProperties(overrides);
        properties.setMessageListener((AcknowledgingMessageListener<String, byte[]>) (record, ack) ->
                receive(endpoint, consumer, record, ack));
        final var container = new ConcurrentMessageListenerContainer<>(consumers, properties);
        container.setConcurrency(kafka.getConcurrency());
        container.setBeanName(endpoint.id());
        // 失败转发未确认时，源 offset 必须保留；不能由默认 recoverer 静默跳过。
        container.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(1000, FixedBackOff.UNLIMITED_ATTEMPTS)));
        customizers.orderedStream().forEach(customizer -> customizer.customize(endpoint, container));
        Assert.state(properties.getAckMode() == ContainerProperties.AckMode.MANUAL_IMMEDIATE,
                "EventDelivery requires Kafka MANUAL_IMMEDIATE ack mode");
        return new EventSubscriptionHandlerImpl(container, endpoint.binding().isAutoStartup());
    }

    private void receive(EventEndpointDto endpoint, EventConsumer consumer,
                         ConsumerRecord<String, byte[]> record, Acknowledgment ack) {
        final var metadata = Option.of(record.headers().lastHeader(EventCodec.METADATA_HEADER))
                .map(Header::value).map(codec::metadata)
                .getOrElse(() -> new EventMetadataDto(record.topic() + ":" + record.partition() + ":" + record.offset(),
                        Map.of(), Map.of(), new EventMetadataDto.RetryDto(0, 0, "")));
        final long remaining = metadata.retry().notBefore() - System.currentTimeMillis();
        if (remaining > 0) {
            ack.nack(Duration.ofMillis(remaining));
            return;
        }
        final var message = new EventMessageDto(metadata, record.value());
        final EventOutcome outcome = consumer.consume(message, List.of(record));
        if (outcome instanceof EventOutcome.Retry retry) {
            send(endpoint.binding().getKafka().getRetryTopic(), record.key(),
                    message.withMetadata(metadata.retryAfter(retry.delay(), retry.reason())), endpoint.binding());
        }
        if (outcome instanceof EventOutcome.Rejected rejected
                && endpoint.binding().getFailure().getAction().equals("dead-letter")) {
            send(endpoint.binding().getFailure().getDestination(), record.key(),
                    message.withMetadata(metadata.rejected(rejected.reason())), endpoint.binding());
        }
        ack.acknowledge();
    }

    @Override
    public void publish(EventBindingProperties binding, EventMessageDto message) {
        Assert.hasText(binding.getKafka().getTopic(), "kafka.topic is required");
        send(binding.getKafka().getTopic(), message.metadata().headers().getOrDefault("event.key", message.metadata().id()),
                message, binding);
    }

    private void send(String topic, String key, EventMessageDto message, EventBindingProperties binding) {
        final var record = new ProducerRecord<String, byte[]>(topic, key, message.payload());
        record.headers().add(EventCodec.METADATA_HEADER, codec.encode(message.metadata()));
        Try.of(() -> publisher.send(record).get(binding.getKafka().getSendTimeout().toMillis(), TimeUnit.MILLISECONDS))
                .getOrElseThrow(cause -> new EventException("Kafka event publication was not confirmed", cause));
    }
}
