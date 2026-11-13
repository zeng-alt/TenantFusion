package com.github.zeng.alt.event.redis;

import com.github.zeng.alt.event.config.EventBindingProperties;
import com.github.zeng.alt.event.spi.EventConsumer;
import com.github.zeng.alt.event.spi.EventEndpointDto;
import com.github.zeng.alt.event.spi.EventMessageDto;
import com.github.zeng.alt.event.spi.EventSubscriptionHandler;
import com.github.zeng.alt.event.spi.EventTransportTemplate;
import com.github.zeng.alt.event.support.EventCodec;
import com.github.zeng.alt.event.support.EventSubscriptionHandlerImpl;

import lombok.RequiredArgsConstructor;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.util.Assert;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 显式的尽力通知模式；不伪造持久化确认或离线补投。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor
public class RedisPubSubEventTemplate implements EventTransportTemplate {
    private final StringRedisTemplate redis;
    private final EventCodec codec;

    @Override
    public String transport() {
        return "redis-pubsub";
    }

    @Override
    public void validate(EventEndpointDto endpoint) {
        Assert.hasText(endpoint.binding().getRedis().getDestination(), "redis.destination is required");
        Assert.isTrue(
                endpoint.binding().getRetry().getMaxAttempts() == 1,
                "Redis Pub/Sub cannot retry reliably; use redis-stream or set max-attempts=1"
        );
        Assert.isTrue(
                endpoint.binding().getFailure().getAction().equals("drop"),
                "Redis Pub/Sub requires explicit failure.action=drop; use redis-stream for reliable delivery"
        );
    }

    @Override
    public EventSubscriptionHandler subscribe(EventEndpointDto endpoint, EventConsumer consumer) {
        final var container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redis.getRequiredConnectionFactory());
        final var settlement = new RedisPubSubEventSettlementHandler();
        container.addMessageListener((message, pattern) -> {
            final EventMessageDto event = codec.decode(message.getBody(), EventMessageDto.class);
            settlement.settle(event, consumer.consume(event, List.of(message)));
        }, new ChannelTopic(endpoint.binding().getRedis().getDestination()));
        container.afterPropertiesSet();
        return new EventSubscriptionHandlerImpl(container, endpoint.binding().isAutoStartup());
    }

    @Override
    public void publish(EventBindingProperties binding, EventMessageDto message) {
        Assert.hasText(binding.getRedis().getDestination(), "redis.destination is required");
        redis.convertAndSend(
                binding.getRedis().getDestination(),
                new String(codec.encode(message), StandardCharsets.UTF_8)
        );
    }
}
