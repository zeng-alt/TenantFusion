package com.github.zeng.alt.message;

/**
 * 消息监听器，用于消费消息的推模式。
 * <p>
 * Message listener for push-based consumption.
 *
 * @param <T> 消息负载类型
 * @author zengJiaJun
 * @since 2026-07-01
 * @version 1.0
 */
@FunctionalInterface
public interface MessageListener<T> {

    /**
     * 收到消息时的回调。
     *
     * @param message 消息体
     */
    void onMessage(Message<T> message);
}
