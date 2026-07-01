package com.github.zeng.alt.message;

/**
 * 消息发送者接口，定义发送消息的契约。
 * <p>
 * Message sender interface that defines the contract for sending messages.
 *
 * @author zengJiaJun
 * @since 2026-07-01
 * @version 1.0
 */
@FunctionalInterface
public interface MessageSender {

    /**
     * 发送消息到指定主题。
     *
     * @param topic   主题 / 路由键
     * @param message 消息原文
     * @param <T>     消息类型
     */
    <T> void send(String topic, Message<T> message);
}
