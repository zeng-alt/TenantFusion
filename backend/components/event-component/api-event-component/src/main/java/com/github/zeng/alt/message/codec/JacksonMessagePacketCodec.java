package com.github.zeng.alt.message.codec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zeng.alt.message.Message;
import com.github.zeng.alt.message.exception.MessageException;

import java.io.IOException;

/**
 * 基于 Jackson 的消息包编解码器。
 * <p>
 * Jackson-based message packet codec.
 * <p>
 * 使用 JSON 格式对 {@link MessagePacket} 进行序列化和反序列化。
 * 是默认的编解码器实现。
 *
 * @author zengJiaJun
 * @since 2026-07-01
 * @version 1.0
 */
public class JacksonMessagePacketCodec implements MessagePacketCodec {

    private final ObjectMapper objectMapper;

    public JacksonMessagePacketCodec() {
        this.objectMapper = new ObjectMapper();
    }

    public JacksonMessagePacketCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public byte[] encode(MessagePacket packet) {
        try {
            return objectMapper.writeValueAsBytes(packet);
        } catch (JsonProcessingException e) {
            throw new MessageException("Failed to encode MessagePacket to JSON", e);
        }
    }

    @Override
    public MessagePacket decode(byte[] data) {
        try {
            return objectMapper.readValue(data, MessagePacket.class);
        } catch (IOException e) {
            throw new MessageException("Failed to decode MessagePacket from JSON", e);
        }
    }

    /**
     * 将 {@link Message} 编码为字节数组。
     *
     * @param topic   主题
     * @param message 消息
     * @param <T>     负载类型
     * @return 编码后的字节
     */
    public <T> byte[] encodeMessage(String topic, Message<T> message) {
        try {
            byte[] payloadBytes = objectMapper.writeValueAsBytes(message.getPayload());
            MessagePacket packet = new MessagePacket(
                    topic,
                    payloadBytes,
                    message.getPayload() != null ? message.getPayload().getClass().getName() : null
            );
            packet.setId(message.getId());
            packet.setTimestamp(message.getTimestamp());
            packet.setHeaders(message.getHeaders());
            return encode(packet);
        } catch (JsonProcessingException e) {
            throw new MessageException("Failed to encode Message to packet bytes", e);
        }
    }

    /**
     * 将字节数组解码为 {@link Message}。
     *
     * @param data  字节数组
     * @param topic 期望的主题（用于校验，可为 null）
     * @param <T>   负载类型
     * @return 消息
     */
    @SuppressWarnings("unchecked")
    public <T> Message<T> decodeMessage(byte[] data, String topic) {
        MessagePacket packet = decode(data);
        if (topic != null && !topic.equals(packet.getTopic())) {
            throw new MessageException("Topic mismatch: expected '" + topic + "', got '" + packet.getTopic() + "'");
        }

        T payload = null;
        if (packet.getPayload() != null && packet.getPayloadClassName() != null) {
            try {
                Class<?> payloadClass = Class.forName(packet.getPayloadClassName());
                payload = (T) objectMapper.readValue(packet.getPayload(), payloadClass);
            } catch (ClassNotFoundException | IOException e) {
                throw new MessageException("Failed to deserialize message payload", e);
            }
        }

        return new Message<>(
                packet.getId(),
                packet.getTopic(),
                payload,
                packet.getTimestamp(),
                packet.getHeaders()
        );
    }
}
