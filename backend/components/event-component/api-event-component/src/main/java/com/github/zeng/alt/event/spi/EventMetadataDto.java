package com.github.zeng.alt.event.spi;

import java.time.Instant;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.Objects;

/**
 * 跨传输的稳定消息 ID、来源快照与重试状态。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public record EventMetadataDto(
        String id,
        Map<String, String> context,
        Map<String, String> headers,
        RetryDto retry
) {
    public EventMetadataDto {
        Objects.requireNonNull(id, "message id");
        context = Map.copyOf(context);
        headers = Map.copyOf(headers);
        Objects.requireNonNull(retry, "retry metadata");
    }
    public static EventMetadataDto create(Map<String, String> context, Map<String, String> headers) {
        return new EventMetadataDto(UUID.randomUUID().toString(), context, headers, new RetryDto(0, 0, ""));
    }
    public EventMetadataDto retryAfter(Duration delay, String reason) {
        return new EventMetadataDto(id, context, headers,
                new RetryDto(retry.count() + 1, Instant.now().plus(delay).toEpochMilli(), reason));
    }
    public EventMetadataDto rejected(String reason) {
        return new EventMetadataDto(id, context, headers, new RetryDto(retry.count(), 0, reason));
    }
    /**
     * 随消息持久化的重试状态，不代表 Broker 的精确投递次数。
     *
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    public record RetryDto(int count, long notBefore, String reason) {
        public RetryDto {
            if (count < 0 || notBefore < 0) {
                throw new IllegalArgumentException("invalid retry metadata");
            }
            Objects.requireNonNull(reason, "retry reason");
        }
    }
}
