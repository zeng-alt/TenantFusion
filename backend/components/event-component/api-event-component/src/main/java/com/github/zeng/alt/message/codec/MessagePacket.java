package com.github.zeng.alt.message.codec;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 消息包（拆包/装包），是消息在网络中传输的序列化单元。
 * <p>
 * Message packet: the serialized unit of a message during transport.
 * <p>
 * <b>拆包机制 / Packet mechanism:</b>
 * <ul>
 *   <li>将 {@code Message&lt;T&gt;} 的负载序列化为 {@code byte[]} 存入此包</li>
 *   <li>携带消息 ID、主题、时间戳、头部等元数据</li>
 *   <li>支持大消息分片：{@link #totalParts} 和 {@link #partIndex}</li>
 * </ul>
 *
 * @author zengJiaJun
 * @since 2026-07-01
 * @version 1.0
 */
public class MessagePacket implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 消息唯一 ID */
    private String id;

    /** 主题 / 路由键 */
    private String topic;

    /** 序列化后的负载字节 */
    private byte[] payload;

    /** 负载的 Java 类全名，用于反序列化 */
    private String payloadClassName;

    /** 创建时间戳 (epoch millis) */
    private long timestamp;

    /** 头部元数据 */
    private Map<String, String> headers;

    /** 总分片数（大消息拆分时使用，默认 1 表示未分片） */
    private int totalParts = 1;

    /** 当前分片索引（从 0 开始） */
    private int partIndex = 0;

    public MessagePacket() {
    }

    /**
     * 创建完整的消息包。
     *
     * @param topic            主题
     * @param payload          序列化后的负载
     * @param payloadClassName 负载类名
     */
    public MessagePacket(String topic, byte[] payload, String payloadClassName) {
        this.id = UUID.randomUUID().toString().replace("-", "");
        this.topic = topic;
        this.payload = payload;
        this.payloadClassName = payloadClassName;
        this.timestamp = Instant.now().toEpochMilli();
        this.headers = Collections.emptyMap();
    }

    // ========== 分片支持 ==========

    /**
     * 是否为分片消息。
     *
     * @return true 如果总分片数大于 1
     */
    public boolean isSplit() {
        return totalParts > 1;
    }

    /**
     * 是否为最后一个分片。
     *
     * @return true 如果是最后一个分片
     */
    public boolean isLastPart() {
        return partIndex == totalParts - 1;
    }

    /**
     * 是否为第一个分片。
     *
     * @return true 如果是第一个分片
     */
    public boolean isFirstPart() {
        return partIndex == 0;
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

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    public String getPayloadClassName() {
        return payloadClassName;
    }

    public void setPayloadClassName(String payloadClassName) {
        this.payloadClassName = payloadClassName;
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

    public int getTotalParts() {
        return totalParts;
    }

    public void setTotalParts(int totalParts) {
        this.totalParts = totalParts;
    }

    public int getPartIndex() {
        return partIndex;
    }

    public void setPartIndex(int partIndex) {
        this.partIndex = partIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MessagePacket that)) return false;
        return timestamp == that.timestamp
                && totalParts == that.totalParts
                && partIndex == that.partIndex
                && Objects.equals(id, that.id)
                && Objects.equals(topic, that.topic)
                && Arrays.equals(payload, that.payload)
                && Objects.equals(payloadClassName, that.payloadClassName)
                && Objects.equals(headers, that.headers);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, topic, payloadClassName, timestamp, headers, totalParts, partIndex);
        result = 31 * result + Arrays.hashCode(payload);
        return result;
    }

    @Override
    public String toString() {
        return "MessagePacket{id='" + id + "', topic='" + topic + "', payloadSize="
                + (payload != null ? payload.length : 0) + ", split=" + isSplit() + "}";
    }
}
