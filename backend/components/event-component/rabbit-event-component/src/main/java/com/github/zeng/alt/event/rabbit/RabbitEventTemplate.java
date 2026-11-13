package com.github.zeng.alt.event.rabbit;

import com.github.zeng.alt.event.EventException;
import com.github.zeng.alt.event.config.EventBindingProperties;
import com.github.zeng.alt.event.spi.EventConsumer;
import com.github.zeng.alt.event.spi.EventEndpointDto;
import com.github.zeng.alt.event.spi.EventMessageDto;
import com.github.zeng.alt.event.spi.EventMetadataDto;
import com.github.zeng.alt.event.spi.EventSubscriptionHandler;
import com.github.zeng.alt.event.spi.EventTransportTemplate;
import com.github.zeng.alt.event.support.EventCodec;
import com.github.zeng.alt.event.support.EventSubscriptionHandlerImpl;
import com.rabbitmq.client.Channel;

import io.vavr.control.Option;
import io.vavr.control.Try;

import lombok.RequiredArgsConstructor;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Rabbit 结果适配；可靠转发确认后确认源消息，延迟通过 TTL 和死信路由实现。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor
public class RabbitEventTemplate implements EventTransportTemplate {
    private final RabbitTemplate publisher;
    private final AmqpAdmin admin;
    private final EventCodec codec;
    private final ObjectProvider<RabbitEventCustomizer> customizers;

    @Override
    public String transport() {
        return "rabbit";
    }

    @Override
    public void validate(EventEndpointDto endpoint) {
        final EventBindingProperties.Rabbit rabbit = endpoint.binding().getRabbit();
        Assert.hasText(rabbit.getQueue(), "rabbit.queue is required");
        Assert.hasText(rabbit.getRetryQueue(), "rabbit.retry-queue is required");
        Assert.isTrue(!rabbit.getQueue().equals(rabbit.getRetryQueue()), "retry queue must differ from source queue");
        Assert.isTrue(
                rabbit.getPrefetch() > 0 && rabbit.getConcurrency() > 0,
                "invalid Rabbit concurrency or prefetch"
        );
        requirePublisherConfirms();
    }

    @Override
    public EventSubscriptionHandler subscribe(EventEndpointDto endpoint, EventConsumer consumer) {
        final EventBindingProperties.Rabbit rabbit = endpoint.binding().getRabbit();
        admin.declareQueue(
                QueueBuilder.durable(rabbit.getRetryQueue()).quorum()
                        .withArgument("x-dead-letter-strategy", "at-least-once")
                        .withArgument("x-overflow", "reject-publish").deadLetterExchange("")
                        .deadLetterRoutingKey(rabbit.getQueue()).build()
        );
        final var container = new SimpleMessageListenerContainer(publisher.getConnectionFactory());
        container.setQueueNames(rabbit.getQueue());
        container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        container.setPrefetchCount(rabbit.getPrefetch());
        container.setConcurrentConsumers(rabbit.getConcurrency());
        container.setMessageListener(
                (ChannelAwareMessageListener) (message, channel) -> receive(endpoint, consumer, message, channel)
        );
        customizers.orderedStream().forEach(customizer -> customizer.customize(endpoint, container));
        Assert.state(
                container.getAcknowledgeMode() == AcknowledgeMode.MANUAL,
                "EventDelivery requires Rabbit MANUAL acknowledgment"
        );
        return new EventSubscriptionHandlerImpl(container, endpoint.binding().isAutoStartup());
    }

    private void receive(EventEndpointDto endpoint, EventConsumer consumer, Message original, Channel channel) {
        final var settlement = new RabbitEventSettlementHandler(
                this, endpoint.binding(), channel, original.getMessageProperties().getDeliveryTag()
        );
        try {
            final byte[] encoded = original.getMessageProperties().getHeader(EventCodec.METADATA_HEADER);
            final EventMetadataDto metadata = Option.of(encoded).map(codec::metadata)
                    .getOrElse(() -> EventMetadataDto.create(Map.of(), Map.of()));
            final var message = new EventMessageDto(metadata, original.getBody());
            settlement.settle(message, consumer.consume(message, List.of(original, channel)));
        } catch (RuntimeException failure) {
            settlement.requeue();
            throw failure;
        }
    }

    @Override
    public void publish(EventBindingProperties binding, EventMessageDto message) {
        requirePublisherConfirms();
        Assert.hasText(binding.getRabbit().routingKey(), "rabbit routing-key or queue is required");
        send(binding.getRabbit().getExchange(), binding.getRabbit().routingKey(), message, binding);
    }

    void send(String exchange, String routingKey, EventMessageDto message, EventBindingProperties binding) {
        final var properties = new MessageProperties();
        properties.setMessageId(message.metadata().id());
        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setHeader(EventCodec.METADATA_HEADER, codec.encode(message.metadata()));
        final long remaining = message.metadata().retry().notBefore() - System.currentTimeMillis();
        if (message.metadata().retry().notBefore() > 0) {
            properties.setExpiration(Long.toString(Math.max(0, remaining)));
        }
        final var outgoing = new Message(message.payload(), properties);
        Assert.state(
                Boolean.TRUE.equals(publisher.isMandatoryFor(outgoing)),
                "Rabbit event publisher requires mandatory=true"
        );
        final var correlation = new CorrelationData();
        publisher.send(exchange, routingKey, outgoing, correlation);
        final CorrelationData.Confirm confirm = Try.of(
                () -> correlation.getFuture()
                        .get(binding.getRabbit().getConfirmTimeout().toMillis(), TimeUnit.MILLISECONDS)
        ).getOrElseThrow(cause -> new EventException("Rabbit event publication was not confirmed", cause));
        if (!confirm.ack() || correlation.getReturned() != null) {
            throw new EventException("Rabbit event publication was rejected or unroutable");
        }
    }

    private void requirePublisherConfirms() {
        Assert.state(
                publisher.getConnectionFactory().isPublisherConfirms()
                        && publisher.getConnectionFactory().isPublisherReturns(),
                "Rabbit events require publisher-confirm-type=correlated and publisher-returns=true"
        );
    }
}
