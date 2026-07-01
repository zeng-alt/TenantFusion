package com.github.zeng.alt.message;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * 消息体，包含消息元数据和负载。
 * <p>
 * Message body, containing metadata and payload.
 * 这是消息收发的基本单位，通过 {@link MessagePacket} 进行序列化传输。
 *
 * @param <T> 消息负载类型
 * @author zengJiaJun
 * @since 2026-07-01
 * @version 1.0
 */
public class Message<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 消息唯一 ID */
    private String id;

    /** 主题 / 路由键 */
    private String topic;

    /** 消息负载 */
    private T payload;

    /** 创建时间戳 (epoch millis) */
    private long timestamp;

    /** 消息头部元数据 */
    private Map<String, String> headers;

    public Message() {
    }

    /**
     * 使用负载快速创建消息。
     *
     * @param topic   主题
     * @param payload 负载
     */
    public Message(String topic, T payload) {
        this.id = UUID.randomUUID().toString().replace("-", "");
        this.topic = topic;
        this.payload = payload;
        this.timestamp = Instant.now().toEpochMilli();
        this.headers = Collections.emptyMap();
    }

    /**
     * 使用完整参数创建消息。
     *
     * @param id        消息 ID
     * @param topic     主题
     * @param payload   负载
     * @param timestamp 时间戳
     * @param headers   头部
     */
    public Message(String id, String topic, T payload, long timestamp, Map<String, String> headers) {
        this.id = id;
        this.topic = topic;
        this.payload = payload;
        this.timestamp = timestamp;
        this.headers = headers != null ? headers : Collections.emptyMap();
    }

    // ========== Getters & Setters ==========

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }

    @SuppressWarnings("unchecked")
    public <R> R getPayloadAs(Class<R> type) {
        return (R) payload;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers != null ? headers : Collections.emptyMap();
    }

    @Override
    public String toString() {
        return "Message{id='" + id + "', topic='" + topic + "', timestamp=" + timestamp + "}";
    }
}
