package com.github.zeng.alt.message.redis;

import com.github.zeng.alt.message.Message;
import com.github.zeng.alt.message.MessageListener;
import com.github.zeng.alt.message.MessageQueueTemplate;
import com.github.zeng.alt.message.codec.JacksonMessagePacketCodec;
import com.github.zeng.alt.message.codec.MessagePacketCodec;
import com.github.zeng.alt.message.exception.MessageException;
import lombok.extern.apachecommons.CommonsLog;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.log.LogMessage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的消息队列模板实现。
 * <p>
 * Redis-based message queue template implementation.
 * <p>
 * <b>实现机制:</b>
 * <ul>
 *   <li><b>发送</b> — 通过 {@link RTopic#publish(Object)} 发布消息</li>
 *   <li><b>拉取</b> — 通过 {@link RBlockingDeque#poll(long, TimeUnit)} 从队列拉取</li>
 *   <li><b>推送</b> — 通过 {@link RTopic#addListener(Class, org.redisson.api.listener.MessageListener)} 监听</li>
 * </ul>
 *
 * @author zengJiaJun
 * @since 2026-07-01
 * @version 1.0
 */
@CommonsLog
public class RedisMessageQueueTemplate implements MessageQueueTemplate, InitializingBean, DisposableBean {

    /**
     * 消息队列的 Redis key 前缀。
     * 用于区分 Pub/Sub 主题和 List 队列。
     */
    private static final String QUEUE_PREFIX = "msg:queue:";

    private final RedissonClient redissonClient;
    private final MessagePacketCodec codec;

    /** 主题 -> (RTopic 监听器 ID) */
    private final Map<String, Integer> subscriptions = new ConcurrentHashMap<>();

    public RedisMessageQueueTemplate(RedissonClient redissonClient) {
        this(redissonClient, new JacksonMessagePacketCodec());
    }

    public RedisMessageQueueTemplate(RedissonClient redissonClient, MessagePacketCodec codec) {
        this.redissonClient = redissonClient;
        this.codec = codec;
    }

    // ========== Send ==========

    @Override
    public <T> void send(String topic, Message<T> message) {
        try {
            byte[] packetBytes = ((JacksonMessagePacketCodec) codec).encodeMessage(topic, message);

            // 通过 Pub/Sub 发布（通知订阅者）
            RTopic rTopic = redissonClient.getTopic(topic);
            rTopic.publish(packetBytes);

            // 同时存入阻塞队列（供拉取消费）
            RBlockingDeque<byte[]> queue = redissonClient.getBlockingDeque(QUEUE_PREFIX + topic);
            queue.offer(packetBytes);

            log.debug(LogMessage.format("Sent message to topic '%s': id=%s", topic, message.getId()));
        } catch (Exception e) {
            throw new MessageException("Failed to send message to topic '" + topic + "'", e);
        }
    }

    // ========== Receive (Pull) ==========

    @Override
    @SuppressWarnings("unchecked")
    public <T> Message<T> receive(String topic) {
        try {
            RBlockingDeque<byte[]> queue = redissonClient.getBlockingDeque(QUEUE_PREFIX + topic);
            byte[] packetBytes = queue.take(); // 阻塞直到有消息
            return ((JacksonMessagePacketCodec) codec).decodeMessage(packetBytes, topic);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            throw new MessageException("Failed to receive message from topic '" + topic + "'", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Message<T> receive(String topic, long timeout, TimeUnit unit) {
        try {
            RBlockingDeque<byte[]> queue = redissonClient.getBlockingDeque(QUEUE_PREFIX + topic);
            byte[] packetBytes = queue.poll(timeout, unit);
            if (packetBytes == null) {
                return null;
            }
            return ((JacksonMessagePacketCodec) codec).decodeMessage(packetBytes, topic);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            throw new MessageException("Failed to receive message from topic '" + topic + "'", e);
        }
    }

    // ========== Subscribe (Push) ==========

    @Override
    @SuppressWarnings("unchecked")
    public <T> void subscribe(String topic, MessageListener<T> listener) {
        RTopic rTopic = redissonClient.getTopic(topic);

        int listenerId = rTopic.addListener(byte[].class, (channel, msg) -> {
            try {
                Message<T> message = ((JacksonMessagePacketCodec) codec).decodeMessage(msg, topic);
                listener.onMessage(message);
            } catch (Exception e) {
                log.error(LogMessage.format("Error processing message from topic '%s'", topic), e);
            }
        });

        subscriptions.put(topic, listenerId);
        log.info(LogMessage.format("Subscribed to topic '%s'", topic));
    }

    @Override
    public void unsubscribe(String topic) {
        Integer listenerId = subscriptions.remove(topic);
        if (listenerId != null) {
            RTopic rTopic = redissonClient.getTopic(topic);
            rTopic.removeListener(listenerId);
            log.info(LogMessage.format("Unsubscribed from topic '%s'", topic));
        }
    }

    @Override
    public void afterPropertiesSet() {
        log.info("RedisMessageQueueTemplate initialized");
    }

    @Override
    public void destroy() {
        subscriptions.forEach((topic, listenerId) -> {
            try {
                RTopic rTopic = redissonClient.getTopic(topic);
                rTopic.removeListener(listenerId);
            } catch (Exception e) {
                log.warn(LogMessage.format("Error removing listener for topic '%s'", topic), e);
            }
        });
        subscriptions.clear();
    }
}
