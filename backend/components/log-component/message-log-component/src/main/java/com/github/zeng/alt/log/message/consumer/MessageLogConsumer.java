package com.github.zeng.alt.log.message.consumer;

import com.github.zeng.alt.log.OperLogEvent;
import com.github.zeng.alt.log.message.producer.MessageLogProducer;
import com.github.zeng.alt.message.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.log.LogMessage;

/**
 * 消息日志消费者。
 * <p>
 * 处理从消息队列接收到的 {@link OperLogEvent}，重新发布到当前应用上下文，
 * 供 {@code jpa-log-component} 等本地处理器持久化。
 * <p>
 * 使用 {@link com.github.zeng.alt.message.annotation.MessageListener @MessageListener}
 * 注解的方式由 {@code api-message-component} 自动注册订阅。
 *
 * @author zengJiaJun
 * @since 2026-07-01
 * @version 1.0
 */
@CommonsLog
@RequiredArgsConstructor
public class MessageLogConsumer {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * 处理从消息队列接收到的操作日志事件。
     * <p>
     * 设置 {@link MessageLogProducer#FROM_MESSAGE} 标记，
     * 防止生产者将事件再次发送回消息队列形成循环。
     */
    @SuppressWarnings("unused")
    public void handleOperLog(Message<OperLogEvent> message) {
        OperLogEvent event = message.getPayload();
        if (event == null) {
            log.warn("Received empty OperLogEvent from message queue");
            return;
        }

        // 设置回流标记，防止生产者再次发送到消息队列
        MessageLogProducer.setFromMessage(true);
        try {
            eventPublisher.publishEvent(event);
            log.debug(LogMessage.format("Re-published OperLogEvent from message queue: title=%s", event.getTitle()));
        } catch (Exception e) {
            log.error("Failed to re-publish OperLogEvent from message queue", e);
        } finally {
            MessageLogProducer.setFromMessage(false);
        }
    }
}
