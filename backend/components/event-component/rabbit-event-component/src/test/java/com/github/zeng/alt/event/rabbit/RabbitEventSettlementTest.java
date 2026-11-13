package com.github.zeng.alt.event.rabbit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zeng.alt.event.EventException;
import com.github.zeng.alt.event.EventOutcome;
import com.github.zeng.alt.event.config.EventBindingProperties;
import com.github.zeng.alt.event.spi.EventEndpointDto;
import com.github.zeng.alt.event.spi.EventSubscriptionHandler;
import com.github.zeng.alt.event.support.EventCodec;
import com.rabbitmq.client.Channel;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Broker 拒绝重试发布时，源投递必须重入队而非确认。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class RabbitEventSettlementTest {
    @Test
    void nacksSourceWhenRetryPublicationIsNotConfirmed() throws Exception {
        final RabbitTemplate publisher = mock(RabbitTemplate.class);
        when(publisher.getConnectionFactory()).thenReturn(mock(ConnectionFactory.class));
        when(publisher.isMandatoryFor(any())).thenReturn(true);
        doAnswer(invocation -> {
            final CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(false, "unavailable"));
            return null;
        }).when(publisher).send(eq(""), eq("orders.retry"), any(Message.class), any(CorrelationData.class));
        final var container = new AtomicReference<SimpleMessageListenerContainer>();
        final var beans = new DefaultListableBeanFactory();
        beans.registerSingleton("capture", (RabbitEventCustomizer) (endpoint, created) -> container.set(created));
        final var transport = new RabbitEventTemplate(
                publisher,
                mock(AmqpAdmin.class),
                new EventCodec(new ObjectMapper()),
                beans.getBeanProvider(RabbitEventCustomizer.class)
        );
        final var binding = new EventBindingProperties();
        binding.getRabbit().setQueue("orders");
        binding.getRabbit().setRetryQueue("orders.retry");
        final EventSubscriptionHandler subscription = transport.subscribe(
                new EventEndpointDto("orders", "orders", binding),
                (message, nativeContexts) -> new EventOutcome.Retry(Duration.ZERO, "temporary")
        );
        final var properties = new MessageProperties();
        properties.setDeliveryTag(7L);
        final ChannelAwareMessageListener listener = (ChannelAwareMessageListener) container.get().getMessageListener();
        final Channel channel = mock(Channel.class);
        assertThatThrownBy(() -> listener.onMessage(new Message(new byte[0], properties), channel))
                .isInstanceOf(EventException.class);
        verify(channel).basicNack(7L, false, true);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        subscription.destroy();
    }
}
