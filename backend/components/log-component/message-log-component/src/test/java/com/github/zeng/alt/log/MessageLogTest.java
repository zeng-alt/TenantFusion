package com.github.zeng.alt.log;

import com.github.zeng.alt.event.EventTemplate;
import com.github.zeng.alt.log.message.config.MessageLogAutoConfiguration;
import com.github.zeng.alt.log.message.consumer.MessageLogConsumer;
import com.github.zeng.alt.log.message.producer.MessageLogProducer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.event.TransactionalEventListenerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 无事务发送、消费回流与消费失败传播契约。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class MessageLogTest {
    @Test
    void sendsLocalEventsWithoutATransaction() {
        final EventTemplate template = mock(EventTemplate.class, RETURNS_DEEP_STUBS);
        runner(template).run(context -> {
            final OperLogEvent event = new OperLogEvent();
            context.publishEvent(event);
            verify(template.to("log-oper")).publish(event);
        });
    }

    @Test
    void doesNotResendConsumedEventsWhenProducerAndConsumerShareAService() {
        final EventTemplate template = mock(EventTemplate.class, RETURNS_DEEP_STUBS);
        runner(template).withPropertyValues("log.message.consumer-enabled=true").run(context -> {
            context.getBean(MessageLogConsumer.class).onOperLog(new OperLogEvent());
            verifyNoInteractions(template);
            assertThat(MessageLogProducer.isFromMessage()).isFalse();
            context.publishEvent(new OperLogEvent());
            verify(template).to("log-oper");
        });
    }

    @Test
    void propagatesPersistenceFailureToTheConsumerAndClearsItsMarker() {
        final EventTemplate template = mock(EventTemplate.class, RETURNS_DEEP_STUBS);
        runner(template).withPropertyValues("log.message.consumer-enabled=true")
                .withBean(RejectingListener.class).run(context -> {
                    assertThatThrownBy(() -> context.getBean(MessageLogConsumer.class).onOperLog(new OperLogEvent()))
                            .hasMessage("database unavailable");
                    assertThat(MessageLogProducer.isFromMessage()).isFalse();
                    verifyNoInteractions(template);
                });
    }

    @Test
    void canDisableBothRoles() {
        runner(mock(EventTemplate.class)).withPropertyValues(
                "log.message.producer-enabled=false", "log.message.consumer-enabled=false"
        ).run(context -> assertThat(context).doesNotHaveBean(MessageLogProducer.class)
                .doesNotHaveBean(MessageLogConsumer.class));
    }

    @Test
    void consumerOnlyDoesNotRequireAProducer() {
        runner(mock(EventTemplate.class)).withPropertyValues(
                "log.message.producer-enabled=false", "log.message.consumer-enabled=true"
        ).run(context -> {
            assertThat(context).doesNotHaveBean(MessageLogProducer.class).hasSingleBean(MessageLogConsumer.class);
            context.getBean(MessageLogConsumer.class).onOperLog(new OperLogEvent());
        });
    }

    @Test
    void isolatesBrokerFailuresFromLocalEventPublishers() {
        final EventTemplate template = mock(EventTemplate.class);
        org.mockito.Mockito.when(template.to("log-oper")).thenThrow(new IllegalStateException("broker unavailable"));
        runner(template).run(context -> {
            context.publishEvent(new OperLogEvent());
            verify(template).to("log-oper");
        });
    }

    private ApplicationContextRunner runner(EventTemplate template) {
        return new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(MessageLogAutoConfiguration.class))
                .withPropertyValues("alt.event.bindings.log-oper.transport=fake")
                .withBean(EventTemplate.class, () -> template).withUserConfiguration(Transactions.class);
    }

    @Configuration(proxyBeanMethods = false)
    static class Transactions {
        @Bean
        static TransactionalEventListenerFactory transactionalEventListenerFactory() {
            return new TransactionalEventListenerFactory();
        }
    }

    static class RejectingListener {
        @EventListener
        public void reject(OperLogEvent event) { throw new IllegalStateException("database unavailable"); }
    }
}
