package com.github.zeng.alt.event.spi;

import com.github.zeng.alt.event.EventOutcome;

/**
 * 单次原生投递的结果处理 SPI；仅在监听方法和托管事务结束后调用。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public interface EventSettlementHandler {

    /** 确认当前投递；Pub/Sub 只能表示本次调用结束，不提供 Broker 确认。 */
    void ack();

    /** 保存包含重试状态的消息，成功后才确认当前投递；失败必须向调用方传播。 */
    void retry(EventMessageDto message, EventOutcome.Retry retry);

    /** 按端点配置转入死信或丢弃；死信发布失败时不得确认当前投递。 */
    void reject(EventMessageDto message, EventOutcome.Rejected rejected);

    /** 将业务结果分派到对应的原生操作，一次投递只调用一次。 */
    default void settle(EventMessageDto message, EventOutcome outcome) {
        switch (outcome) {
            case EventOutcome.Acknowledged acknowledged -> ack();
            case EventOutcome.Retry retry -> retry(message, retry);
            case EventOutcome.Rejected rejected -> reject(message, rejected);
        }
    }
}
