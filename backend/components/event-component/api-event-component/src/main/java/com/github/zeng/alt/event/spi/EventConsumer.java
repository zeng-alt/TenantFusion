package com.github.zeng.alt.event.spi;

import com.github.zeng.alt.event.EventOutcome;

import java.util.List;

/**
 * 传输适配器调用业务监听器的边界。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@FunctionalInterface
public interface EventConsumer {
    EventOutcome consume(EventMessageDto message, List<Object> nativeContexts);
}
