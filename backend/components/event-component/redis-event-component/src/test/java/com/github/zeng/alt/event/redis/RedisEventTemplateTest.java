package com.github.zeng.alt.event.redis;

import com.github.zeng.alt.event.EventTemplate;
import com.github.zeng.alt.event.support.EventCodec;
import com.github.zeng.alt.event.spi.EventMetadataDto;
import com.github.zeng.alt.event.testing.EventBrokerTestSupport;
import com.github.zeng.alt.event.testing.EventBrokerTestSupport.Listener;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import java.time.Duration;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import com.github.zeng.alt.event.spi.EventMessageDto;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 真实 Broker 上的统一监听、上下文、重试和死信验证。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class RedisEventTemplateTest {
    private static final GenericContainer<?> BROKER = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);
    @BeforeAll static void startBroker() { BROKER.start(); }
    @AfterAll static void stopBroker() { BROKER.stop(); }

    @Test
    void retriesStreamWithContextAndClearsPending() {
        final String stream = "retry-" + UUID.randomUUID();
        final var listener = new Listener();
        runner(stream, listener).run(context -> {
            assertThat(context).hasNotFailed();
            final String id = EventBrokerTestSupport.publish(context.getBean(EventTemplate.class), "retry");
            EventBrokerTestSupport.assertAttempts(listener, id, 2);
            final var redis = context.getBean(StringRedisTemplate.class);
            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                assertThat(redis.opsForStream().pending(stream, "orders").getTotalPendingMessages()).isZero();
                assertThat(redis.opsForStream().pending(stream + ":retry:orders", "orders").getTotalPendingMessages()).isZero();
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
            final var redis = context.getBean(StringRedisTemplate.class);
            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(redis.opsForStream().size(stream + ".dead"))
                    .isEqualTo(1));
            final var records = redis.opsForStream().range(stream + ".dead", org.springframework.data.domain.Range.unbounded());
            final var dead = context.getBean(EventCodec.class).decode(
                    records.getFirst().getValue().get("event").toString().getBytes(StandardCharsets.UTF_8), EventMessageDto.class);
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
            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(
                    context.getBean(StringRedisTemplate.class).opsForStream().size(stream + ":retry:orders")).isEqualTo(1));
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
        runner(channel, listener).withPropertyValues("alt.event.bindings.test.transport=redis-pubsub",
                "alt.event.bindings.test.retry.max-attempts=1", "alt.event.bindings.test.failure.action=drop")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    final String id = EventBrokerTestSupport.publish(context.getBean(EventTemplate.class), "ack");
                    EventBrokerTestSupport.assertAttempts(listener, id, 1);
                });
    }

    @Test
    void rejectsReliableRetryConfigurationForPubSubAtStartup() {
        runner("invalid-" + UUID.randomUUID(), new Listener())
                .withPropertyValues("alt.event.bindings.test.transport=redis-pubsub")
                .run(context -> assertThat(context).hasFailed().getFailure()
                        .hasMessageContaining("Redis Pub/Sub cannot retry reliably"));
    }

    private ApplicationContextRunner runner(String stream, Listener listener) {
        final var connection = new LettuceConnectionFactory(BROKER.getHost(), BROKER.getMappedPort(6379));
        connection.afterPropertiesSet();
        final var redis = new StringRedisTemplate(connection);
        return EventBrokerTestSupport.runner(listener)
                .withConfiguration(AutoConfigurations.of(RedisEventAutoConfiguration.class))
                .withBean(LettuceConnectionFactory.class, () -> connection)
                .withBean(StringRedisTemplate.class, () -> redis)
                .withPropertyValues("alt.event.bindings.test.transport=redis-stream",
                        "alt.event.bindings.test.redis.destination=" + stream,
                        "alt.event.bindings.test.redis.group=orders",
                        "alt.event.bindings.test.redis.claim-idle=100ms",
                        "alt.event.bindings.test.redis.poll-interval=50ms",
                        "alt.event.bindings.test.failure.destination=" + stream + ".dead");
    }
}
