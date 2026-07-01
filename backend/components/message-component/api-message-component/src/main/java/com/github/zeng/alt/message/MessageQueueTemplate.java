package com.github.zeng.alt.message;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 消息队列模板，提供完整的消息收发操作。
 * <p>
 * Message queue template providing send, receive, and subscribe operations.
 * <p>
 * 支持三种消费模式：
 * <ul>
 *   <li><b>发送</b> — 通过 {@link #send(String, Message)}</li>
 *   <li><b>拉取</b> — 通过 {@link #receive(String)} 主动拉取</li>
 *   <li><b>推送</b> — 通过 {@link #subscribe(String, MessageListener)} 注册监听器</li>
 * </ul>
 *
 * @author zengJiaJun
 * @since 2026-07-01
 * @version 1.0
 */
public interface MessageQueueTemplate {

    // ========== 发送 ==========

    /**
     * 发送消息到指定主题。
     *
     * @param topic   主题
     * @param payload 消息负载
     * @param <T>     负载类型
     */
    default <T> void send(String topic, T payload) {
        send(topic, new Message<>(topic, payload));
    }

    /**
     * 发送消息到指定主题，携带头部信息。
     *
     * @param topic   主题
     * @param payload 消息负载
     * @param headers 头部元数据
     * @param <T>     负载类型
     */
    default <T> void send(String topic, T payload, Map<String, String> headers) {
        Message<T> message = new Message<>(topic, payload);
        message.setHeaders(headers);
        send(topic, message);
    }

    /**
     * 发送完整消息体。
     *
     * @param topic   主题
     * @param message 消息体
     * @param <T>     负载类型
     */
    <T> void send(String topic, Message<T> message);

    // ========== 拉取消费 ==========

    /**
     * 从指定主题拉取一条消息（阻塞等待）。
     *
     * @param topic 主题
     * @param <T>   负载类型
     * @return 消息体，超时或空时返回 {@code null}
     */
    <T> Message<T> receive(String topic);

    /**
     * 从指定主题拉取一条消息（指定超时）。
     *
     * @param topic   主题
     * @param timeout 超时时间
     * @param unit    时间单位
     * @param <T>     负载类型
     * @return 消息体，超时时返回 {@code null}
     */
    <T> Message<T> receive(String topic, long timeout, TimeUnit unit);

    // ========== 推送消费 ==========

    /**
     * 订阅指定主题，注册监听器。
     *
     * @param topic    主题
     * @param listener 消息监听器
     * @param <T>      负载类型
     */
    <T> void subscribe(String topic, MessageListener<T> listener);

    /**
     * 取消订阅指定主题。
     *
     * @param topic 主题
     */
    void unsubscribe(String topic);
}
