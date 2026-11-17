package com.github.zeng.alt.log.jpa.event;

import com.github.zeng.alt.log.OperLogEvent;
import com.github.zeng.alt.log.jpa.service.LogPersistenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;

/**
 * 操作日志持久化监听器。
 * <p>
 * 监听 {@link OperLogEvent}，将日志保存到数据库。
 * 收到事件后立即委派独立事务保存，消费失败交由发布边界处理。
 *
 * @author zengJiaJun
 * @since 2026-07-01
 * @version 1.0
 */
@RequiredArgsConstructor
public class LogEventPersistenceListener {
    private final LogPersistenceService persistence;

    @EventListener
    public void handleOperLog(OperLogEvent event) {
        persistence.create(event);
    }
}
