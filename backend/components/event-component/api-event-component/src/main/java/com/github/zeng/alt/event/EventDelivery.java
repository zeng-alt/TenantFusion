package com.github.zeng.alt.event;

import java.time.Duration;

/**
 * 创建消费结果的统一接口；终结方法返回意图，不提前确认消息。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public interface EventDelivery {
    /** 获取本次调用的来源信息；租户和安全上下文已在调用监听方法前恢复。 */
    EventContext context();

    /** 标记处理成功；适配器在监听调用及托管事务结束后确认源消息。 */
    EventOutcome ack();

    /** 按绑定的退避策略请求重试，重试预算包含首次投递。 */
    EventOutcome retry();

    /** 请求至少等待指定时长后重试；仍受绑定的重试预算约束。 */
    EventOutcome retry(Duration delay);

    /** 终止重试，按绑定配置转入死信或丢弃，并回滚托管事务。 */
    EventOutcome reject(String reason);
}
