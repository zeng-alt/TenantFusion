package com.github.zeng.alt.message.codec;

/**
 * 消息包编解码器接口。
 * <p>
 * Message packet codec interface.
 * <p>
 * 负责 {@link MessagePacket} 与字节数组之间的互相转换，
 * 以便在 Redis / RabbitMQ / Kafka 等消息中间件中传输。
 *
 * @author zengJiaJun
 * @since 2026-07-01
 * @version 1.0
 */
public interface MessagePacketCodec {

    /**
     * 将消息包编码为字节数组。
     *
     * @param packet 消息包
     * @return 字节数组
     */
    byte[] encode(MessagePacket packet);

    /**
     * 将字节数组解码为消息包。
     *
     * @param data 字节数组
     * @return 消息包
     */
    MessagePacket decode(byte[] data);
}
