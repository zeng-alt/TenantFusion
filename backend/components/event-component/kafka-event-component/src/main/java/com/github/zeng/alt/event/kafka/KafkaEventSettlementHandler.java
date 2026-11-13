package com.github.zeng.alt.event.kafka;

import com.github.zeng.alt.event.EventOutcome;
import com.github.zeng.alt.event.config.EventBindingProperties;
import com.github.zeng.alt.event.spi.EventMessageDto;
import com.github.zeng.alt.event.spi.EventSettlementHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.support.Acknowledgment;

import java.time.Duration;

/**
 * Kafka 单次投递的确认与重投；可靠发送成功后才提交源 offset。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor
public class KafkaEventSettlementHandler implements EventSettlementHandler {

    private final KafkaEventTemplate publisher;
    private final EventBindingProperties binding;
    private final Acknowledgment acknowledgment;
    private final String key;

    @Override
    public void ack() {
        acknowledgment.acknowledge();
    }

    @Override
    public void retry(EventMessageDto message, EventOutcome.Retry retry) {
        publisher.send(
                binding.getKafka().getRetryTopic(),
                key,
                message.withMetadata(message.metadata().retryAfter(retry.delay(), retry.reason())),
                binding
        );
        ack();
    }

    @Override
    public void reject(EventMessageDto message, EventOutcome.Rejected rejected) {
        if (binding.getFailure().getAction().equals("dead-letter")) {
            publisher.send(
                    binding.getFailure().getDestination(),
                    key,
                    message.withMetadata(message.metadata().rejected(rejected.reason())),
                    binding
            );
        }
        ack();
    }

    /** 重试消息尚未到期时保留 offset，并通过容器的 nack 暂停后续拉取。 */
    public void defer(Duration delay) {
        acknowledgment.nack(delay);
    }
}
