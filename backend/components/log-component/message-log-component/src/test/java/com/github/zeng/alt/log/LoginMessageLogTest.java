package com.github.zeng.alt.log;

import com.github.zeng.alt.event.EventOutcome;
import com.github.zeng.alt.event.EventTemplate;
import com.github.zeng.alt.event.config.EventAutoConfiguration;
import com.github.zeng.alt.event.config.EventBindingProperties;
import com.github.zeng.alt.event.config.EventContextAutoConfiguration;
import com.github.zeng.alt.event.spi.EventConsumer;
import com.github.zeng.alt.event.spi.EventEndpointDto;
import com.github.zeng.alt.event.spi.EventMessageDto;
import com.github.zeng.alt.event.spi.EventSubscriptionHandler;
import com.github.zeng.alt.event.spi.EventTransportTemplate;
import com.github.zeng.alt.log.message.config.MessageLogAutoConfiguration;
import com.github.zeng.alt.log.message.consumer.LoginMessageLogConsumer;
import com.github.zeng.alt.log.message.producer.LoginMessageLogProducer;
import com.github.zeng.alt.log.message.producer.MessageLogProducer;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.event.EventListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 使用真实编解码与订阅链路验证登录日志消息、租户恢复和消费失败处理。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class LoginMessageLogTest {
    @AfterEach
    void clearContext() {
        TenantContextHolder.clear();
        MessageLogProducer.setFromMessage(false);
    }

    @Test
    void roundTripsLoginMetadataAndTenantWithoutResendingConsumedEvents() {
        final var transport = new FakeTransport();
        runner(transport).withBean(Records.class).run(context -> {
            assertThat(context).hasNotFailed();
            TenantContextHolder.setTenantId("tenant-a");
            final LoginInfoEvent event = login();
            context.publishEvent(event);
            assertThat(transport.sent).hasSize(1);
            TenantContextHolder.setTenantId("consumer-tenant");
            assertThat(transport.deliver()).isInstanceOf(EventOutcome.Acknowledged.class);
            final Records records = context.getBean(Records.class);
            assertThat(records.events()).hasSize(2);
            assertThat(records.events().getLast()).isEqualTo(event).isNotSameAs(event);
            assertThat(records.tenants().getLast()).isEqualTo("tenant-a");
            assertThat(TenantContextHolder.getTenantId()).isEqualTo("consumer-tenant");
            assertThat(transport.sent).hasSize(1);
            assertThat(MessageLogProducer.isFromMessage()).isFalse();
            context.publishEvent(login());
            assertThat(transport.sent).hasSize(2);
        });
    }

    @Test
    void returnsRetryWhenTheLocalLoginSinkFailsAndClearsTheLoopMarker() {
        final var transport = new FakeTransport();
        runner(transport).withBean(RejectingListener.class).run(context -> {
            context.getBean(EventTemplate.class).to("log-login").publish(login());
            assertThat(transport.deliver()).isInstanceOf(EventOutcome.Retry.class);
            assertThat(MessageLogProducer.isFromMessage()).isFalse();
            assertThat(transport.sent).hasSize(1);
        });
    }

    @Test
    void supportsConsumerOnlyAndCanDisableBothRoles() {
        runner(new FakeTransport()).withPropertyValues("log.message.producer-enabled=false").run(context -> {
            assertThat(context).hasSingleBean(LoginMessageLogConsumer.class)
                    .doesNotHaveBean(LoginMessageLogProducer.class);
        });
        runner(new FakeTransport()).withPropertyValues(
                "log.message.producer-enabled=false", "log.message.consumer-enabled=false"
        ).run(context -> assertThat(context).doesNotHaveBean(LoginMessageLogProducer.class)
                .doesNotHaveBean(LoginMessageLogConsumer.class));
    }

    @Test
    void isolatesLoginBrokerFailures() {
        final EventTemplate template = mock(EventTemplate.class);
        when(template.to("log-login")).thenThrow(new IllegalStateException("broker unavailable"));
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(MessageLogAutoConfiguration.class))
                .withBean(EventTemplate.class, () -> template)
                .withPropertyValues("alt.event.bindings.log-login.transport=fake").run(context -> {
                    context.publishEvent(login());
                    verify(template).to("log-login");
                });
    }

    @Test
    void registersBothLogSubscriptionsWhenBothBindingsAreConfigured() {
        final var transport = new FakeTransport();
        runner(transport).withPropertyValues(
                "alt.event.bindings.log-oper.transport=fake", "alt.event.bindings.log-oper.failure.action=drop"
        ).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(transport.consumers).containsOnlyKeys("operation-log", "login-log");
            context.publishEvent(login());
            context.publishEvent(new OperLogEvent());
            assertThat(transport.sent).hasSize(2);
            assertThat(transport.deliver()).isInstanceOf(EventOutcome.Acknowledged.class);
            assertThat(transport.sent).hasSize(2);
        });
    }

    @Test
    void doesNotRegisterMessageRolesWithoutAnyBinding() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(MessageLogAutoConfiguration.class))
                .withBean(EventTemplate.class, () -> mock(EventTemplate.class))
                .withPropertyValues("log.message.consumer-enabled=true")
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(MessageLogProducer.class)
                        .doesNotHaveBean(LoginMessageLogConsumer.class).doesNotHaveBean(LoginMessageLogProducer.class));
    }

    @Test
    void leavesExistingOperationOnlyApplicationsWithoutLoginBindingsUnchanged() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(MessageLogAutoConfiguration.class))
                .withBean(EventTemplate.class, () -> mock(EventTemplate.class))
                .withPropertyValues("alt.event.bindings.log-oper.transport=fake", "log.message.consumer-enabled=true")
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(MessageLogProducer.class)
                        .doesNotHaveBean(LoginMessageLogConsumer.class).doesNotHaveBean(LoginMessageLogProducer.class));
    }

    private LoginInfoEvent login() {
        final LoginInfoEvent event = new LoginInfoEvent();
        event.setUsername("alice");
        event.setTenantId("tenant-a");
        event.setStatus("0");
        event.setMessage("登录成功");
        event.setIp("113.92.157.29");
        event.setUserAgent("test-agent");
        event.setBrowser("Chrome 131");
        event.setOs("Windows 10");
        event.setLoginLocation("中国 广东 深圳");
        event.setLoginTime(LocalDateTime.of(2024, 1, 2, 3, 4, 5));
        return event;
    }

    private ApplicationContextRunner runner(FakeTransport transport) {
        return new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(
                EventContextAutoConfiguration.class, EventAutoConfiguration.class, MessageLogAutoConfiguration.class
        )).withBean(EventTransportTemplate.class, () -> transport).withPropertyValues(
                "alt.event.bindings.log-login.transport=fake",
                "alt.event.bindings.log-login.failure.action=drop",
                "log.message.consumer-enabled=true"
        );
    }

    /**
     * 同时记录事件负载与消费时线程的租户。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    static class Records {
        final List<LoginInfoEvent> events = new ArrayList<>();
        final List<String> tenants = new ArrayList<>();

        List<LoginInfoEvent> events() { return List.copyOf(events); }
        List<String> tenants() { return List.copyOf(tenants); }

        @EventListener
        public void login(LoginInfoEvent event) {
            events.add(event);
            tenants.add(TenantContextHolder.getTenantId());
        }
    }

    /**
     * 模拟落库失败。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    static class RejectingListener {
        @EventListener
        public void reject(LoginInfoEvent event) { throw new IllegalStateException("database unavailable"); }
    }

    /**
     * 只替代 Broker I/O，保留组件的真实序列化、上下文和订阅逻辑。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    static class FakeTransport implements EventTransportTemplate {
        final Map<String, EventConsumer> consumers = new LinkedHashMap<>();
        final List<EventMessageDto> sent = new ArrayList<>();

        @Override
        public String transport() { return "fake"; }

        @Override
        public void validate(EventEndpointDto endpoint) { }

        @Override
        public EventSubscriptionHandler subscribe(EventEndpointDto endpoint, EventConsumer consumer) {
            consumers.put(endpoint.id(), consumer);
            return new Handler();
        }

        @Override
        public void publish(EventBindingProperties binding, EventMessageDto message) { sent.add(message); }

        EventOutcome deliver() { return consumers.get("login-log").consume(sent.getFirst(), List.of()); }
    }

    /**
     * 测试订阅生命周期。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    static class Handler implements EventSubscriptionHandler {
        private boolean running;
        @Override public void start() { running = true; }
        @Override public void stop() { running = false; }
        @Override public boolean isRunning() { return running; }
    }
}
