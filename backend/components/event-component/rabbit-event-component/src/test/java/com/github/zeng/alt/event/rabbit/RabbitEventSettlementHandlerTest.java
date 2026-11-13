package com.github.zeng.alt.event.rabbit;

import com.github.zeng.alt.event.EventException;
import com.github.zeng.alt.event.EventOutcome;
import com.github.zeng.alt.event.config.EventBindingProperties;
import com.github.zeng.alt.event.spi.EventMessageDto;
import com.github.zeng.alt.event.spi.EventMetadataDto;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InOrder;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 验证重试和死信的源确认必须晚于可靠转发成功。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class RabbitEventSettlementHandlerTest {

    private final RabbitEventTemplate publisher = mock(RabbitEventTemplate.class);
    private final Channel channel = mock(Channel.class);
    private final EventBindingProperties binding = new EventBindingProperties();
    private final EventMessageDto message = new EventMessageDto(
            EventMetadataDto.create(Map.of("request.tenantId", "tenant-a"), Map.of()),
            new byte[0]
    );

    @BeforeEach
    void configureDestinations() {
        binding.getRabbit().setRetryQueue("orders.retry");
        binding.getFailure().setDestination("orders.dead");
    }

    @ParameterizedTest
    @MethodSource("forwardingOutcomes")
    void acknowledgesOnlyAfterForwardingSucceeds(EventOutcome outcome) throws Exception {
        final var handler = new RabbitEventSettlementHandler(publisher, binding, channel, 7L);

        handler.settle(message, outcome);

        final InOrder order = inOrder(publisher, channel);
        order.verify(publisher).send(eq(""), any(String.class), any(EventMessageDto.class), eq(binding));
        order.verify(channel).basicAck(7L, false);
        order.verifyNoMoreInteractions();
    }

    @ParameterizedTest
    @MethodSource("forwardingOutcomes")
    void leavesSourceUnacknowledgedWhenForwardingFails(EventOutcome outcome) throws Exception {
        doThrow(new EventException("publication failed"))
                .when(publisher).send(eq(""), any(String.class), any(EventMessageDto.class), eq(binding));
        final var handler = new RabbitEventSettlementHandler(publisher, binding, channel, 7L);

        assertThatThrownBy(() -> handler.settle(message, outcome)).isInstanceOf(EventException.class);

        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    void explicitlyDroppedRejectionAcknowledgesWithoutForwarding() throws Exception {
        binding.getFailure().setAction("drop");
        final var handler = new RabbitEventSettlementHandler(publisher, binding, channel, 7L);

        handler.settle(message, new EventOutcome.Rejected("unsupported order"));

        verify(channel).basicAck(7L, false);
        verifyNoInteractions(publisher);
    }

    private static Stream<EventOutcome> forwardingOutcomes() {
        return Stream.of(
                new EventOutcome.Retry(Duration.ZERO, "temporary"),
                new EventOutcome.Rejected("unsupported order")
        );
    }
}
