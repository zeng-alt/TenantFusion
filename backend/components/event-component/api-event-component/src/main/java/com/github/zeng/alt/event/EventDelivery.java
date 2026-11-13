package com.github.zeng.alt.event;

import java.time.Duration;

/**
 * 创建消费结果的统一接口；终结方法返回意图，不提前确认消息。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public interface EventDelivery {
    EventContext context();
    EventOutcome ack();
    EventOutcome retry();
    EventOutcome retry(Duration delay);
    EventOutcome reject(String reason);
}
