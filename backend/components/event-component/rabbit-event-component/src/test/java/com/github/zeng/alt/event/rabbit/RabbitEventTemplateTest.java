package com.github.zeng.alt.event.rabbit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.github.zeng.alt.event.EventTemplate;
import com.github.zeng.alt.event.spi.EventMetadataDto;
import com.github.zeng.alt.event.support.EventCodec;
import com.github.zeng.alt.event.testing.EventBrokerTestSupport;
import com.github.zeng.alt.event.testing.EventBrokerTestSupport.Listener;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/**
 * 真实 Broker 上的统一监听、上下文、重试和死信验证。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class RabbitEventTemplateTest {
    private static final GenericContainer<?> BROKER = new GenericContainer<>(
            DockerImageName.parse("rabbitmq:3.13-alpine")
    ).withExposedPorts(5672);

    @BeforeAll
    static void startBroker() {
        BROKER.start();
    }

    @AfterAll
    static void stopBroker() {
        BROKER.stop();
    }

    @Test
    void retriesWithOriginalContextAndConfirmsSuccess() {
        final var listener = new Listener();
        runner("retry-" + UUID.randomUUID(), listener).run(context -> {
            assertThat(context).hasNotFailed();
            final String id = EventBrokerTestSupport.publish(context.getBean(EventTemplate.class), "retry");
            EventBrokerTestSupport.assertAttempts(listener, id, 2);
        });
    }

    @Test
    void rejectsToDeadLetterWithoutRetry() {
        final String queue = "reject-" + UUID.randomUUID();
        final var listener = new Listener();
        runner(queue, listener).run(context -> {
            assertThat(context).hasNotFailed();
            final String id = EventBrokerTestSupport.publish(context.getBean(EventTemplate.class), "reject");
            EventBrokerTestSupport.assertAttempts(listener, id, 1);
            final Message dead = await().atMost(Duration.ofSeconds(20))
                    .until(() -> context.getBean(RabbitTemplate.class).receive(queue + ".dead"), Objects::nonNull);
            final EventMetadataDto metadata = context.getBean(EventCodec.class)
                    .metadata(dead.getMessageProperties().getHeader(EventCodec.METADATA_HEADER));
            assertThat(metadata.id()).isEqualTo(id);
            assertThat(metadata.retry().reason()).isEqualTo("unsupported order");
        });
    }

    @Test
    void exhaustsSharedRetryBudgetBeforeDeadLetter() {
        final String queue = "budget-" + UUID.randomUUID();
        final var listener = new Listener();
        runner(queue, listener).run(context -> {
            assertThat(context).hasNotFailed();
            final String id = EventBrokerTestSupport.publish(context.getBean(EventTemplate.class), "fail");
            EventBrokerTestSupport.assertAttempts(listener, id, 3);
            final Message dead = await().atMost(Duration.ofSeconds(20))
                    .until(() -> context.getBean(RabbitTemplate.class).receive(queue + ".dead"), Objects::nonNull);
            final EventMetadataDto metadata = context.getBean(EventCodec.class)
                    .metadata(dead.getMessageProperties().getHeader(EventCodec.METADATA_HEADER));
            assertThat(metadata.id()).isEqualTo(id);
            assertThat(metadata.retry().count()).isEqualTo(2);
        });
    }

    private ApplicationContextRunner runner(String queue, Listener listener) {
        final var connection = new CachingConnectionFactory(BROKER.getHost(), BROKER.getMappedPort(5672));
        connection.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        connection.setPublisherReturns(true);
        final var publisher = new RabbitTemplate(connection);
        publisher.setMandatory(true);
        final var admin = new RabbitAdmin(connection);
        admin.declareQueue(new Queue(queue, true));
        admin.declareQueue(new Queue(queue + ".dead", true));
        return EventBrokerTestSupport.runner(listener)
                .withConfiguration(AutoConfigurations.of(RabbitEventAutoConfiguration.class))
                .withBean(CachingConnectionFactory.class, () -> connection)
                .withBean(RabbitTemplate.class, () -> publisher).withBean(AmqpAdmin.class, () -> admin)
                .withPropertyValues(
                        "alt.event.bindings.test.transport=rabbit",
                        "alt.event.bindings.test.rabbit.queue=" + queue,
                        "alt.event.bindings.test.rabbit.retry-queue=" + queue + ".retry",
                        "alt.event.bindings.test.failure.destination=" + queue + ".dead"
                );
    }
}
