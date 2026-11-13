package com.github.zeng.alt.event.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.github.zeng.alt.event.EventTemplate;
import com.github.zeng.alt.event.spi.EventMessageDto;
import com.github.zeng.alt.event.support.EventCodec;
import com.github.zeng.alt.event.testing.EventBrokerTestSupport;
import com.github.zeng.alt.event.testing.EventBrokerTestSupport.Listener;

import io.vavr.control.Option;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 真实 Broker 上的统一监听、上下文、重试和死信验证。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class RedisEventTemplateTest {
    private static final Option<String> EXTERNAL_HOST = Option.of(System.getenv("EVENT_TEST_REDIS_HOST"))
            .filter(host -> !host.isBlank());
    private static final GenericContainer<?> BROKER = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);
    private final Set<String> destinations = new HashSet<>();

    @BeforeAll
    static void startBroker() {
        if (EXTERNAL_HOST.isEmpty()) {
            BROKER.start();
        }
    }

    @AfterAll
    static void stopBroker() {
        if (EXTERNAL_HOST.isEmpty()) {
            BROKER.stop();
        }
    }

    @AfterEach
    void deleteTestStreams() {
        final LettuceConnectionFactory connection = connection();
        try {
            final StringRedisTemplate redis = new StringRedisTemplate(connection);
            destinations.forEach(
                    destination -> redis
                            .delete(List.of(destination, destination + ":retry:orders", destination + ".dead"))
            );
        } finally {
            connection.destroy();
        }
    }

    @Test
    void retriesStreamWithContextAndClearsPending() {
        final String stream = "retry-" + UUID.randomUUID();
        final var listener = new Listener();
        runner(stream, listener).run(context -> {
            assertThat(context).hasNotFailed();
            final String id = EventBrokerTestSupport.publish(context.getBean(EventTemplate.class), "retry");
            EventBrokerTestSupport.assertAttempts(listener, id, 2);
            final StringRedisTemplate redis = context.getBean(StringRedisTemplate.class);
            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                assertThat(redis.opsForStream().pending(stream, "orders").getTotalPendingMessages()).isZero();
                assertThat(redis.opsForStream().pending(stream + ":retry:orders", "orders").getTotalPendingMessages())
                        .isZero();
            });
        });
    }

    @Test
    void exhaustsRetryBudgetAndPersistsDeadLetter() {
        final String stream = "budget-" + UUID.randomUUID();
        final var listener = new Listener();
        runner(stream, listener).run(context -> {
            assertThat(context).hasNotFailed();
            final String id = EventBrokerTestSupport.publish(context.getBean(EventTemplate.class), "fail");
            EventBrokerTestSupport.assertAttempts(listener, id, 3);
            final StringRedisTemplate redis = context.getBean(StringRedisTemplate.class);
            await().atMost(Duration.ofSeconds(10))
                    .untilAsserted(() -> assertThat(redis.opsForStream().size(stream + ".dead")).isEqualTo(1));
            final List<MapRecord<String, Object, Object>> records = redis.opsForStream()
                    .range(stream + ".dead", org.springframework.data.domain.Range.unbounded());
            final EventMessageDto dead = context.getBean(EventCodec.class).decode(
                    records.getFirst().getValue().get("event").toString().getBytes(StandardCharsets.UTF_8),
                    EventMessageDto.class
            );
            assertThat(dead.metadata().id()).isEqualTo(id);
            assertThat(dead.metadata().retry().count()).isEqualTo(2);
        });
    }

    @Test
    void resumesPersistedRetryAfterApplicationRestart() {
        final String stream = "restart-" + UUID.randomUUID();
        final var listener = new Listener();
        final var id = new AtomicReference<String>();
        runner(stream, listener).run(context -> {
            assertThat(context).hasNotFailed();
            id.set(EventBrokerTestSupport.publish(context.getBean(EventTemplate.class), "restart"));
            await().atMost(Duration.ofSeconds(10))
                    .untilAsserted(
                            () -> assertThat(
                                    context.getBean(StringRedisTemplate.class).opsForStream()
                                            .size(stream + ":retry:orders")
                            ).isEqualTo(1)
                    );
        });
        runner(stream, listener).run(context -> {
            assertThat(context).hasNotFailed();
            EventBrokerTestSupport.assertAttempts(listener, id.get(), 2);
        });
    }

    @Test
    void pubSubRestoresContextWithoutClaimingReliableDelivery() {
        final String channel = "notify-" + UUID.randomUUID();
        final var listener = new Listener();
        runner(channel, listener).withPropertyValues(
                "alt.event.bindings.test.transport=redis-pubsub",
                "alt.event.bindings.test.retry.max-attempts=1",
                "alt.event.bindings.test.failure.action=drop"
        ).run(context -> {
            assertThat(context).hasNotFailed();
            final String id = EventBrokerTestSupport.publish(context.getBean(EventTemplate.class), "ack");
            EventBrokerTestSupport.assertAttempts(listener, id, 1);
        });
    }

    @Test
    void rejectsReliableRetryConfigurationForPubSubAtStartup() {
        runner("invalid-" + UUID.randomUUID(), new Listener())
                .withPropertyValues("alt.event.bindings.test.transport=redis-pubsub").run(
                        context -> assertThat(context).hasFailed().getFailure()
                                .hasMessageContaining("Redis Pub/Sub cannot retry reliably")
                );
    }

    private ApplicationContextRunner runner(String stream, Listener listener) {
        destinations.add(stream);
        final LettuceConnectionFactory connection = connection();
        final StringRedisTemplate redis = new StringRedisTemplate(connection);
        return EventBrokerTestSupport.runner(listener)
                .withConfiguration(AutoConfigurations.of(RedisEventAutoConfiguration.class))
                .withBean(LettuceConnectionFactory.class, () -> connection)
                .withBean(StringRedisTemplate.class, () -> redis).withPropertyValues(
                        "alt.event.bindings.test.transport=redis-stream",
                        "alt.event.bindings.test.redis.destination=" + stream,
                        "alt.event.bindings.test.redis.group=orders",
                        "alt.event.bindings.test.redis.claim-idle=100ms",
                        "alt.event.bindings.test.redis.poll-interval=50ms",
                        "alt.event.bindings.test.failure.destination=" + stream + ".dead"
                );
    }

    private LettuceConnectionFactory connection() {
        final String host = EXTERNAL_HOST.getOrElse(BROKER::getHost);
        final int port = EXTERNAL_HOST.map(
                ignored -> Option.of(System.getenv("EVENT_TEST_REDIS_PORT")).map(Integer::parseInt).getOrElse(6379)
        ).getOrElse(() -> BROKER.getMappedPort(6379));
        final LettuceConnectionFactory connection = new LettuceConnectionFactory(host, port);
        connection.afterPropertiesSet();
        return connection;
    }
}
