package com.github.zeng.alt.log.message.consumer;

import com.github.zeng.alt.log.OperLogEvent;
import com.github.zeng.alt.message.Message;
import com.github.zeng.alt.message.MessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;

/**
 * 将 {@link MessageHandler} 接口桥接到 {@link MessageLogConsumer}。
 * <p>
 * Bridges the {@link MessageHandler} interface to {@link MessageLogConsumer}.
 * <p>
 * 当消息模块的 {@code MessageListenerBeanPostProcessor} 扫描到此 Bean 时，
 * 自动订阅 {@code log.oper} 主题。
 *
 * @author zengJiaJun
 * @since 2026-07-01
 * @version 1.0
 */
@CommonsLog
@RequiredArgsConstructor
public class MessageLogMessageHandler implements MessageHandler<OperLogEvent> {

    /** 日志消息主题 */
    static final String TOPIC = "log.oper";

    private final MessageLogConsumer consumer;

    @Override
    public String getTopic() {
        return TOPIC;
    }

    @Override
    public void onMessage(Message<OperLogEvent> message) {
        consumer.handleOperLog(message);
    }
}
