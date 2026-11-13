package com.github.zeng.alt.event.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zeng.alt.event.EventException;
import com.github.zeng.alt.event.EventOutcome;
import com.github.zeng.alt.event.config.EventBindingProperties;
import com.github.zeng.alt.event.spi.EventEndpointDto;
import com.github.zeng.alt.event.support.EventCodec;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.support.Acknowledgment;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 重试发布失败时不能提交源消息 offset。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class KafkaEventSettlementTest {
    @Test
    @SuppressWarnings("unchecked")
    void leavesSourceUnacknowledgedWhenRetryPublicationFails() {
        final var publisher = (KafkaTemplate<String, byte[]>) mock(KafkaTemplate.class);
        final var consumers = (ConsumerFactory<String, byte[]>) mock(ConsumerFactory.class);
        when(consumers.getConfigurationProperties()).thenReturn(Map.of());
        when(publisher.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker unavailable")));
        final var container = new AtomicReference<ConcurrentMessageListenerContainer<String, byte[]>>();
        final var beans = new DefaultListableBeanFactory();
        beans.registerSingleton("capture", (KafkaEventCustomizer) (endpoint, created) -> container.set(created));
        final var transport = new KafkaEventTemplate(publisher, consumers, new EventCodec(new ObjectMapper()),
                beans.getBeanProvider(KafkaEventCustomizer.class));
        final var binding = new EventBindingProperties();
        binding.getKafka().setTopic("orders");
        binding.getKafka().setRetryTopic("orders.retry");
        binding.getKafka().setGroupId("inventory");
        final var subscription = transport.subscribe(new EventEndpointDto("orders", "orders", binding),
                (message, nativeContexts) -> new EventOutcome.Retry(Duration.ZERO, "temporary"));
        final var listener = (AcknowledgingMessageListener<String, byte[]>) container.get()
                .getContainerProperties().getMessageListener();
        final var acknowledgment = mock(Acknowledgment.class);
        assertThatThrownBy(() -> listener.onMessage(new ConsumerRecord<>("orders", 0, 1L, "key", new byte[0]),
                acknowledgment)).isInstanceOf(EventException.class);
        verifyNoInteractions(acknowledgment);
        subscription.destroy();
    }
}
