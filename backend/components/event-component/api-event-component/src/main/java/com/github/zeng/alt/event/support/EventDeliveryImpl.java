package com.github.zeng.alt.event.support;

import com.github.zeng.alt.event.EventContext;
import com.github.zeng.alt.event.EventDelivery;
import com.github.zeng.alt.event.EventOutcome;
import java.time.Duration;

/**
 * 创建处理意图，不允许业务在事务完成前直接确认 Broker。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public record EventDeliveryImpl(EventContext context, Duration defaultDelay) implements EventDelivery {
    @Override public EventOutcome ack() { return new EventOutcome.Acknowledged(); }
    @Override public EventOutcome retry() { return retry(defaultDelay); }
    @Override public EventOutcome retry(Duration delay) { return new EventOutcome.Retry(delay, "retry requested"); }
    @Override public EventOutcome reject(String reason) { return new EventOutcome.Rejected(reason); }
}
