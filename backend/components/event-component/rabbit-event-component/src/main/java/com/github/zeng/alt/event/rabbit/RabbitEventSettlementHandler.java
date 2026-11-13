package com.github.zeng.alt.event.rabbit;

import com.github.zeng.alt.event.EventException;
import com.github.zeng.alt.event.EventOutcome;
import com.github.zeng.alt.event.config.EventBindingProperties;
import com.github.zeng.alt.event.spi.EventMessageDto;
import com.github.zeng.alt.event.spi.EventSettlementHandler;
import com.rabbitmq.client.Channel;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;

/**
 * RabbitMQ 单次投递的确认与重投；发布确认成功后才确认源 delivery tag。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor
public class RabbitEventSettlementHandler implements EventSettlementHandler {

    private final RabbitEventTemplate publisher;
    private final EventBindingProperties binding;
    private final Channel channel;
    private final long deliveryTag;

    @Override
    public void ack() {
        Try.run(() -> channel.basicAck(deliveryTag, false))
                .getOrElseThrow(cause -> new EventException("Cannot acknowledge Rabbit event", cause));
    }

    @Override
    public void retry(EventMessageDto message, EventOutcome.Retry retry) {
        publisher.send(
                "",
                binding.getRabbit().getRetryQueue(),
                message.withMetadata(message.metadata().retryAfter(retry.delay(), retry.reason())),
                binding
        );
        ack();
    }

    @Override
    public void reject(EventMessageDto message, EventOutcome.Rejected rejected) {
        if (binding.getFailure().getAction().equals("dead-letter")) {
            publisher.send(
                    "",
                    binding.getFailure().getDestination(),
                    message.withMetadata(message.metadata().rejected(rejected.reason())),
                    binding
            );
        }
        ack();
    }

    /** 解析或转发失败时将源投递重新入队，供容器再次消费。 */
    public void requeue() {
        Try.run(() -> channel.basicNack(deliveryTag, false, true))
                .getOrElseThrow(cause -> new EventException("Cannot requeue Rabbit event", cause));
    }
}
