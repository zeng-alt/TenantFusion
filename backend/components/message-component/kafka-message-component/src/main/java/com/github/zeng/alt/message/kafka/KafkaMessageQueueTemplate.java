package com.github.zeng.alt.message.kafka;

import com.github.zeng.alt.message.Message;
import com.github.zeng.alt.message.MessageListener;
import com.github.zeng.alt.message.MessageQueueTemplate;
import com.github.zeng.alt.message.codec.JacksonMessagePacketCodec;
import com.github.zeng.alt.message.codec.MessagePacketCodec;
import com.github.zeng.alt.message.exception.MessageException;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Kafka 的消息队列模板实现。
 * <p>
 * Kafka-based message queue template implementation.
 * <p>
 * <b>实现机制:</b>
 * <ul>
 *   <li><b>发送</b> — 使用 {@link KafkaTemplate#send(ProducerRecord)}</li>
 *   <li><b>拉取</b> — 使用 {@link KafkaConsumer#poll(Duration)}</li>
 *   <li><b>推送</b> — 使用 {@link ConcurrentMessageListenerContainer}</li>
 * </ul>
 * <p>
 * 拉取模式使用独立消费者 (group.id = "pull-" + UUID)，每次拉取后关闭。
 *
 * @author zengJiaJun
 * @since 2026-07-01
 * @version 1.0
 */
public class KafkaMessageQueueTemplate implements MessageQueueTemplate, InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(KafkaMessageQueueTemplate.class);

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final ConsumerFactory<String, byte[]> consumerFactory;
    private final KafkaAdmin kafkaAdmin;
    private final MessagePacketCodec codec;

    /** 主题 -> 监听器容器映射 */
    private final Map<String, ConcurrentMessageListenerContainer<String, byte[]>> containers = new ConcurrentHashMap<>();

    public KafkaMessageQueueTemplate(
            KafkaTemplate<String, byte[]> kafkaTemplate,
            ConsumerFactory<String, byte[]> consumerFactory,
            KafkaAdmin kafkaAdmin) {
        this(kafkaTemplate, consumerFactory, kafkaAdmin, new JacksonMessagePacketCodec());
    }

    public KafkaMessageQueueTemplate(
            KafkaTemplate<String, byte[]> kafkaTemplate,
            ConsumerFactory<String, byte[]> consumerFactory,
            KafkaAdmin kafkaAdmin,
            MessagePacketCodec codec) {
        this.kafkaTemplate = kafkaTemplate;
        this.consumerFactory = consumerFactory;
        this.kafkaAdmin = kafkaAdmin;
        this.codec = codec;
    }

    // ========== Send ==========

    @Override
    public <T> void send(String topic, Message<T> message) {
        try {
            byte[] packetBytes = ((JacksonMessagePacketCodec) codec).encodeMessage(topic, message);
            kafkaTemplate.send(topic, message.getId(), packetBytes).get(10, TimeUnit.SECONDS);
            log.debug("Sent message to topic '{}': id={}, partition={}",
                    topic, message.getId());
        } catch (Exception e) {
            throw new MessageException("Failed to send message to topic '" + topic + "'", e);
        }
    }

    // ========== Receive (Pull) ==========

    @Override
    @SuppressWarnings("unchecked")
    public <T> Message<T> receive(String topic) {
        return receive(topic, 5, TimeUnit.SECONDS);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Message<T> receive(String topic, long timeout, TimeUnit unit) {
        // 为拉取模式创建临时消费者
        try (Consumer<String, byte[]> consumer = consumerFactory.createConsumer(
                "pull-" + UUID.randomUUID().toString().replace("-", ""),
                "-")) {
            consumer.subscribe(Collections.singletonList(topic));
            ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(unit.toMillis(timeout)));

            for (ConsumerRecord<String, byte[]> record : records) {
                Message<T> message = ((JacksonMessagePacketCodec) codec).decodeMessage(record.value(), topic);
                // 提交偏移量
                consumer.commitSync();
                return message;
            }
        } catch (Exception e) {
            throw new MessageException("Failed to receive message from topic '" + topic + "'", e);
        }
        return null;
    }

    // ========== Subscribe (Push) ==========

    @Override
    @SuppressWarnings("unchecked")
    public <T> void subscribe(String topic, MessageListener<T> listener) {
        ContainerProperties containerProps = new ContainerProperties(topic);
        containerProps.setGroupId("sub-" + topic);
        containerProps.setMessageListener((org.springframework.kafka.listener.MessageListener<byte[], byte[]>) record -> {
            try {
                Message<T> message = ((JacksonMessagePacketCodec) codec).decodeMessage(record.value(), topic);
                listener.onMessage(message);
            } catch (Exception e) {
                log.error("Error processing message from topic '{}'", topic, e);
            }
        });

        ConcurrentMessageListenerContainer<String, byte[]> container =
                new ConcurrentMessageListenerContainer<>(consumerFactory, containerProps);
        container.start();

        containers.put(topic, container);
        log.info("Subscribed to topic '{}'", topic);
    }

    @Override
    public void unsubscribe(String topic) {
        ConcurrentMessageListenerContainer<String, byte[]> container = containers.remove(topic);
        if (container != null) {
            try {
                container.stop();
                container.destroy();
            } catch (Exception e) {
                log.warn("Error stopping container for topic '{}'", topic, e);
            }
            log.info("Unsubscribed from topic '{}'", topic);
        }
    }

    @Override
    public void afterPropertiesSet() {
        log.info("KafkaMessageQueueTemplate initialized");
    }

    @Override
    public void destroy() {
        containers.forEach((topic, container) -> {
            try {
                container.stop();
                container.destroy();
            } catch (Exception e) {
                log.warn("Error stopping container for topic '{}'", topic, e);
            }
        });
        containers.clear();
    }
}
