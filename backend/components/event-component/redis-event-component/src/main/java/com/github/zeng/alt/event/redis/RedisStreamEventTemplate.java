package com.github.zeng.alt.event.redis;

import com.github.zeng.alt.event.config.EventBindingProperties;
import com.github.zeng.alt.event.spi.EventConsumer;
import com.github.zeng.alt.event.spi.EventEndpointDto;
import com.github.zeng.alt.event.spi.EventMessageDto;
import com.github.zeng.alt.event.spi.EventSubscriptionHandler;
import com.github.zeng.alt.event.spi.EventTransportTemplate;
import com.github.zeng.alt.event.support.EventCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.Assert;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Redis Streams 发送与订阅适配。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor
public class RedisStreamEventTemplate implements EventTransportTemplate {
    private final StringRedisTemplate redis;
    private final EventCodec codec;
    private final ObjectProvider<RedisEventCustomizer> customizers;
    @Override public String transport() { return "redis-stream"; }
    @Override public void validate(EventEndpointDto endpoint) {
        final var options = endpoint.binding().getRedis();
        Assert.hasText(options.getDestination(), "redis.destination is required");
        Assert.hasText(options.getGroup(), "redis.group is required for Streams");
        Assert.isTrue(options.getBatchSize() > 0, "redis.batch-size must be positive");
        Assert.isTrue(options.getPollInterval().toMillis() > 0, "redis.poll-interval must be positive");
        Assert.isTrue(options.getClaimIdle().toMillis() > 0, "redis.claim-idle must be positive");
    }
    @Override public EventSubscriptionHandler subscribe(EventEndpointDto endpoint, EventConsumer consumer) {
        return new RedisStreamSubscriptionHandler(redis, codec, customizers, endpoint, consumer);
    }
    @Override public void publish(EventBindingProperties binding, EventMessageDto message) {
        Assert.hasText(binding.getRedis().getDestination(), "redis.destination is required");
        redis.opsForStream().add(binding.getRedis().getDestination(),
                Map.of("event", new String(codec.encode(message), StandardCharsets.UTF_8)));
    }
}
