package com.github.zeng.alt.event.subscription;

import com.github.zeng.alt.event.EventContext;
import com.github.zeng.alt.event.EventOutcome;
import com.github.zeng.alt.event.EventTemplate;
import com.github.zeng.alt.event.annotation.EventSubscription;
import com.github.zeng.alt.event.config.EventAutoConfiguration;
import com.github.zeng.alt.event.config.EventBindingProperties;
import com.github.zeng.alt.event.config.EventContextAutoConfiguration;
import com.github.zeng.alt.event.spi.EventConsumer;
import com.github.zeng.alt.event.spi.EventEndpointDto;
import com.github.zeng.alt.event.spi.EventMessageDto;
import com.github.zeng.alt.event.spi.EventSubscriptionHandler;
import com.github.zeng.alt.event.spi.EventTransportTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.messaging.handler.annotation.Header;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 注册、代理方法、泛型参数、端点身份和能力校验。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class EventSubscriptionTemplateTest {
    private ApplicationContextRunner runner(FakeTransport transport) {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(EventContextAutoConfiguration.class, EventAutoConfiguration.class))
                .withBean(EventTransportTemplate.class, () -> transport)
                .withPropertyValues("alt.event.bindings.orders.transport=fake",
                        "alt.event.bindings.orders.failure.action=drop");
    }

    @Test
    void discoversJdkProxyAndResolvesGenericPayloadHeadersAndContext() {
        final var transport = new FakeTransport();
        final var target = new OrderListener();
        final var proxy = new ProxyFactory(target);
        proxy.setInterfaces(Orders.class);
        runner(transport).withBean("orders", Orders.class, () -> (Orders) proxy.getProxy()).run(context -> {
            assertThat(context).hasNotFailed();
            context.getBean(EventTemplate.class).to("orders").header("attempt", "7")
                    .publish(List.of(new Order("A"), new Order("B")));
            assertThat(transport.deliver("orders-listener")).isInstanceOf(EventOutcome.Acknowledged.class);
            assertThat(target.ids).containsExactly("A", "B");
            assertThat(target.attempt).isEqualTo(7);
            assertThat(target.context.listenerId()).isEqualTo("orders-listener");
            assertThat(target.context.nativeContext(String.class).get()).isEqualTo("native");
            assertThat(target.context.nativeContext(Integer.class)).isEmpty();
        });
    }

    @Test
    void registersTwoListenersForSameBindingAndControlsThemById() {
        final var transport = new FakeTransport();
        runner(transport).withBean(OrderListener.class).withBean(SecondListener.class).run(context -> {
            assertThat(context).hasNotFailed();
            final var subscriptions = context.getBean(EventSubscriptionTemplate.class);
            assertThat(subscriptions.ids()).containsExactlyInAnyOrder("orders-listener", "second-listener");
            assertThat(transport.handlers.get("orders-listener").isRunning()).isTrue();
            subscriptions.stop("orders-listener");
            assertThat(transport.handlers.get("orders-listener").isRunning()).isFalse();
            assertThat(transport.handlers.get("second-listener").isRunning()).isTrue();
            subscriptions.start("orders-listener");
            assertThat(transport.handlers.get("orders-listener").isRunning()).isTrue();
        });
    }

    @Test
    void rejectsDuplicateListenerIdDuringStartup() {
        runner(new FakeTransport()).withBean("first", OrderListener.class).withBean("second", OrderListener.class)
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().hasMessageContaining("Duplicate or empty event listener id"));
    }

    @Test
    void rejectsUnknownBindingDuringStartup() {
        runner(new FakeTransport()).withBean(UnknownListener.class)
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().hasMessageContaining("Unknown event binding"));
    }

    @Test
    void doesNotAcknowledgeNullOutcome() {
        final var transport = new FakeTransport();
        runner(transport).withBean(NullListener.class)
                .withPropertyValues("alt.event.bindings.orders.retry.max-attempts=1").run(context -> {
                    assertThat(context).hasNotFailed();
                    context.getBean(EventTemplate.class).to("orders").publish(new Order("A"));
                    assertThat(transport.deliver("null-listener")).isInstanceOf(EventOutcome.Rejected.class);
                });
    }

    /**
     * 验证实际代理的调用接口。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    public interface Orders {
        void receive(List<Order> orders, int attempt, EventContext context);
    }

    /**
     * 泛型监听方法，只在实现类标注。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    public static class OrderListener implements Orders {
        final List<String> ids = new ArrayList<>();
        int attempt;
        EventContext context;
        @Override
        @EventSubscription(id = "orders-listener", binding = "orders")
        public void receive(List<Order> orders, @Header("attempt") int attempt, EventContext context) {
            orders.forEach(order -> ids.add(order.id()));
            this.attempt = attempt;
            this.context = context;
        }
    }

    /**
     * 第二个相同 binding 的监听器。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    public static class SecondListener {
        @EventSubscription(id = "second-listener", binding = "orders")
        public void receive() { }
    }

    /**
     * 不存在的绑定。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    public static class UnknownListener {
        @EventSubscription(id = "unknown", binding = "missing")
        public void receive(Order order) { }
    }

    /**
     * 非法结果不得成为确认。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    public static class NullListener {
        @EventSubscription(id = "null-listener", binding = "orders")
        public EventOutcome receive(Order order) { return null; }
    }

    /**
     * 测试负载。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    public record Order(String id) { }

    /**
     * 隔离注册行为的测试传输。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    static class FakeTransport implements EventTransportTemplate {
        final Map<String, EventConsumer> consumers = new LinkedHashMap<>();
        final Map<String, Handler> handlers = new LinkedHashMap<>();
        EventMessageDto message;
        @Override public String transport() { return "fake"; }
        @Override public void validate(EventEndpointDto endpoint) { }
        @Override public EventSubscriptionHandler subscribe(EventEndpointDto endpoint, EventConsumer consumer) {
            consumers.put(endpoint.id(), consumer);
            final var handler = new Handler();
            handlers.put(endpoint.id(), handler);
            return handler;
        }
        @Override public void publish(EventBindingProperties binding, EventMessageDto message) { this.message = message; }
        EventOutcome deliver(String id) { return consumers.get(id).consume(message, List.of("native")); }
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
