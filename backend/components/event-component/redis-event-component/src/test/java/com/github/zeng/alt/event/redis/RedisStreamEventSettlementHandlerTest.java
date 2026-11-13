package com.github.zeng.alt.event.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zeng.alt.event.EventOutcome;
import com.github.zeng.alt.event.config.EventBindingProperties;
import com.github.zeng.alt.event.spi.EventMessageDto;
import com.github.zeng.alt.event.spi.EventMetadataDto;
import com.github.zeng.alt.event.support.EventCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InOrder;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 Streams 先保存重投消息，再确认源 pending 项。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class RedisStreamEventSettlementHandlerTest {

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final StreamOperations<String, String, String> streams = mock(StreamOperations.class);
    private final EventBindingProperties binding = new EventBindingProperties();
    private final EventCodec codec = new EventCodec(new ObjectMapper());
    private final MapRecord<String, String, String> record = MapRecord.create("orders", Map.of("event", "{}"))
            .withId(RecordId.of("1-0"));
    private final EventMessageDto message = new EventMessageDto(
            EventMetadataDto.create(Map.of("request.tenantId", "tenant-a"), Map.of()),
            new byte[0]
    );

    @BeforeEach
    void configureStreams() {
        binding.getRedis().setDestination("orders");
        binding.getRedis().setGroup("inventory");
        binding.getFailure().setDestination("orders.dead");
        when(redis.<String, String>opsForStream()).thenReturn(streams);
    }

    @ParameterizedTest
    @MethodSource("forwardingOutcomes")
    void acknowledgesOnlyAfterAppendingSucceeds(EventOutcome outcome) {
        final var handler = new RedisStreamEventSettlementHandler(redis, codec, binding, record);

        handler.settle(message, outcome);

        final InOrder order = inOrder(streams);
        order.verify(streams).add(any(String.class), anyMap());
        order.verify(streams).acknowledge("orders", "inventory", record.getId());
        order.verifyNoMoreInteractions();
    }

    @ParameterizedTest
    @MethodSource("forwardingOutcomes")
    void retainsPendingWhenAppendingFails(EventOutcome outcome) {
        when(streams.add(any(String.class), anyMap())).thenThrow(new IllegalStateException("Redis unavailable"));
        final var handler = new RedisStreamEventSettlementHandler(redis, codec, binding, record);

        assertThatThrownBy(() -> handler.settle(message, outcome)).isInstanceOf(IllegalStateException.class);

        verify(streams, never()).acknowledge(eq("orders"), eq("inventory"), any(RecordId.class));
    }

    @Test
    void explicitlyDroppedRejectionAcknowledgesWithoutAppending() {
        binding.getFailure().setAction("drop");
        final var handler = new RedisStreamEventSettlementHandler(redis, codec, binding, record);

        handler.settle(message, new EventOutcome.Rejected("unsupported order"));

        verify(streams).acknowledge("orders", "inventory", record.getId());
        verify(streams, never()).add(any(String.class), anyMap());
    }

    private static Stream<EventOutcome> forwardingOutcomes() {
        return Stream.of(
                new EventOutcome.Retry(Duration.ZERO, "temporary"),
                new EventOutcome.Rejected("unsupported order")
        );
    }
}
