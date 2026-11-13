package com.github.zeng.alt.event.redis;

import com.github.zeng.alt.event.EventException;
import com.github.zeng.alt.event.EventOutcome;
import com.github.zeng.alt.event.spi.EventMessageDto;
import com.github.zeng.alt.event.spi.EventSettlementHandler;

/**
 * Pub/Sub 的处理结果通知；没有 Broker ACK，失败只交给容器报告，不执行可靠重投。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public class RedisPubSubEventSettlementHandler implements EventSettlementHandler {

    @Override
    public void ack() {
        // Pub/Sub 没有待确认记录，业务成功时无需调用 Broker。
    }

    @Override
    public void retry(EventMessageDto message, EventOutcome.Retry retry) {
        throw new EventException("Redis Pub/Sub cannot retry reliably: " + message.metadata().id());
    }

    @Override
    public void reject(EventMessageDto message, EventOutcome.Rejected rejected) {
        throw new EventException("Pub/Sub event was not processed successfully: " + message.metadata().id());
    }
}
