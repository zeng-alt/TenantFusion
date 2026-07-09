package com.github.zeng.alt.log.message.producer;

import com.github.zeng.alt.log.OperLogEvent;
import com.github.zeng.alt.message.MessageQueueTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.core.log.LogMessage;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 消息日志生产者。
 * <p>
 * 监听本地发布的 {@link OperLogEvent}，通过 {@link MessageQueueTemplate}
 * 转发到消息队列，供其他服务消费。
 * <p>
 * 使用内部标记避免重复转发从消息队列回流的日志事件。
 *
 * @author zengJiaJun
 * @since 2026-07-01
 * @version 1.0
 */
@CommonsLog
@RequiredArgsConstructor
public class MessageLogProducer {

    /** 标记事件是否来自消息队列回流，防止循环转发 */
    private static final ThreadLocal<Boolean> FROM_MESSAGE = ThreadLocal.withInitial(() -> false);

    /**
     * 设置"来自消息队列"标记。消费者在重新发布事件前调用此方法，
     * 生产者会跳过被标记的事件，防止无限循环。
     */
    public static void setFromMessage(boolean fromMessage) {
        if (fromMessage) {
            FROM_MESSAGE.set(true);
        } else {
            FROM_MESSAGE.remove();
        }
    }

    private final MessageQueueTemplate messageQueue;

    /**
     * 检查当前事件是否来自消息队列回流。
     */
    public static boolean isFromMessage() {
        return Boolean.TRUE.equals(FROM_MESSAGE.get());
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onOperLog(OperLogEvent event) {
        // 跳过从消息队列回流的事件，防止循环
        if (isFromMessage()) {
            return;
        }

        try {
            messageQueue.send("log.oper", event);
            log.debug(LogMessage.format("Sent OperLogEvent to message queue: title=%s", event.getTitle()));
        } catch (Exception e) {
            log.error("Failed to send OperLogEvent to message queue", e);
        }
    }
}
