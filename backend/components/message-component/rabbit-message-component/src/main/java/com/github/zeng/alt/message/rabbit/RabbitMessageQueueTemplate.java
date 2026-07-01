package com.github.zeng.alt.message.rabbit;

import com.github.zeng.alt.message.Message;
import com.github.zeng.alt.message.MessageListener;
import com.github.zeng.alt.message.MessageQueueTemplate;
import com.github.zeng.alt.message.codec.JacksonMessagePacketCodec;
import com.github.zeng.alt.message.codec.MessagePacketCodec;
import com.github.zeng.alt.message.exception.MessageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 基于 RabbitMQ 的消息队列模板实现。
 * <p>
 * RabbitMQ-based message queue template implementation.
 * <p>
 * <b>实现机制:</b>
 * <ul>
 *   <li><b>发送</b> — 使用 {@link RabbitTemplate#convertAndSend(String, Object)}</li>
 *   <li><b>拉取</b> — 使用 {@link RabbitTemplate#receive(String)}</li>
 *   <li><b>推送</b> — 使用 {@link SimpleMessageListenerContainer}</li>
 * </ul>
 * <p>
 * 每个主题对应一个 RabbitMQ Queue，使用 topic 作为 queue name。
 *
 * @author zengJiaJun
 * @since 2026-07-01
 * @version 1.0
 */
public class RabbitMessageQueueTemplate implements MessageQueueTemplate, InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(RabbitMessageQueueTemplate.class);

    private final RabbitTemplate rabbitTemplate;
    private final AmqpAdmin amqpAdmin;
    private final MessagePacketCodec codec;

    /** 监听器容器 -> 主题的映射，用于取消订阅 */
    private final Map<String, SimpleMessageListenerContainer> containers = new ConcurrentHashMap<>();

    public RabbitMessageQueueTemplate(RabbitTemplate rabbitTemplate, AmqpAdmin amqpAdmin) {
        this(rabbitTemplate, amqpAdmin, new JacksonMessagePacketCodec());
    }

    public RabbitMessageQueueTemplate(RabbitTemplate rabbitTemplate, AmqpAdmin amqpAdmin, MessagePacketCodec codec) {
        this.rabbitTemplate = rabbitTemplate;
        this.amqpAdmin = amqpAdmin;
        this.codec = codec;
    }

    // ========== Send ==========

    @Override
    public <T> void send(String topic, Message<T> message) {
        try {
            byte[] packetBytes = ((JacksonMessagePacketCodec) codec).encodeMessage(topic, message);
            ensureQueue(topic);
            rabbitTemplate.convertAndSend(topic, packetBytes);
            log.debug("Sent message to topic '{}': id={}", topic, message.getId());
        } catch (AmqpException e) {
            throw new MessageException("Failed to send message to topic '" + topic + "'", e);
        }
    }

    // ========== Receive (Pull) ==========

    @Override
    @SuppressWarnings("unchecked")
    public <T> Message<T> receive(String topic) {
        ensureQueue(topic);
        org.springframework.amqp.core.Message amqpMessage = rabbitTemplate.receive(topic);
        if (amqpMessage == null) {
            return null;
        }
        return ((JacksonMessagePacketCodec) codec).decodeMessage(amqpMessage.getBody(), topic);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Message<T> receive(String topic, long timeout, TimeUnit unit) {
        ensureQueue(topic);
        org.springframework.amqp.core.Message amqpMessage = rabbitTemplate.receive(topic, unit.toMillis(timeout));
        if (amqpMessage == null) {
            return null;
        }
        return ((JacksonMessagePacketCodec) codec).decodeMessage(amqpMessage.getBody(), topic);
    }

    // ========== Subscribe (Push) ==========

    @Override
    @SuppressWarnings("unchecked")
    public <T> void subscribe(String topic, MessageListener<T> listener) {
        ensureQueue(topic);

        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(rabbitTemplate.getConnectionFactory());
        container.setQueueNames(topic);
        container.setMessageListener((ChannelAwareMessageListener) (amqpMessage, channel) -> {
            try {
                Message<T> message = ((JacksonMessagePacketCodec) codec).decodeMessage(amqpMessage.getBody(), topic);
                listener.onMessage(message);
                channel.basicAck(amqpMessage.getMessageProperties().getDeliveryTag(), false);
            } catch (Exception e) {
                log.error("Error processing message from topic '{}'", topic, e);
                channel.basicNack(amqpMessage.getMessageProperties().getDeliveryTag(), false, true);
            }
        });
        container.start();

        containers.put(topic, container);
        log.info("Subscribed to topic '{}'", topic);
    }

    @Override
    public void unsubscribe(String topic) {
        SimpleMessageListenerContainer container = containers.remove(topic);
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

    // ========== Queue Management ==========

    /**
     * 确保队列存在。
     */
    private void ensureQueue(String topic) {
        Queue queue = amqpAdmin.getQueueProperties(topic) == null
                ? null
                : new Queue(topic, true);
        if (queue == null) {
            amqpAdmin.declareQueue(new Queue(topic, true));
        }
    }

    @Override
    public void afterPropertiesSet() {
        log.info("RabbitMessageQueueTemplate initialized");
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
