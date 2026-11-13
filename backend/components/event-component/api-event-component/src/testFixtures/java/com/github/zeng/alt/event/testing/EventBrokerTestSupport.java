package com.github.zeng.alt.event.testing;

import com.github.zeng.alt.event.EventDelivery;
import com.github.zeng.alt.event.EventOutcome;
import com.github.zeng.alt.event.EventTemplate;
import com.github.zeng.alt.event.annotation.EventSubscription;
import com.github.zeng.alt.event.config.EventAutoConfiguration;
import com.github.zeng.alt.event.config.EventContextAutoConfiguration;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.api.UserContextHolder;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * 三种 Broker 共用的黑盒业务观察；读取实际 Holder，不读取测试线程里的快照。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public final class EventBrokerTestSupport {
    private EventBrokerTestSupport() { }

    public static ApplicationContextRunner runner(Listener listener) {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(EventContextAutoConfiguration.class, EventAutoConfiguration.class))
                .withBean("listener", Listener.class, () -> listener)
                .withPropertyValues("alt.event.bindings.test.retry.initial-delay=100ms",
                        "alt.event.bindings.test.retry.max-attempts=3");
    }

    public static String publish(EventTemplate events, String action) {
        TenantContextHolder.setTenantId("tenant-a");
        UserContextHolder.set(UsernamePasswordAuthenticationToken.authenticated(
                SecurityUser.withUsername("alice").id("42").build(), null, List.of()));
        try {
            return events.to("test").key("order-1").header("source", "contract-test").publish(new Payload(action));
        } finally {
            TenantContextHolder.clear();
            SecurityContextHolder.clearContext();
        }
    }

    public static void assertAttempts(Listener listener, String id, int count) {
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(listener.observations(id)).hasSize(count));
        assertThat(listener.observations(id)).allSatisfy(observation -> {
            assertThat(observation.tenant()).isEqualTo("tenant-a");
            assertThat(observation.userId()).isEqualTo("42");
            assertThat(observation.username()).isEqualTo("alice");
            assertThat(observation.explicitTenant()).isEqualTo("tenant-a");
        });
        assertThat(listener.observations(id)).extracting(Observation::retryCount)
                .containsExactlyElementsOf(java.util.stream.IntStream.range(0, count).boxed().toList());
    }

    /**
     * 无基类、上下文字段或消息框架依赖的负载。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    public record Payload(String action) { }

    /**
     * 跨线程观察。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    public record Observation(String id, int retryCount, String tenant, String userId, String username,
                              String explicitTenant) { }

    /**
     * 同一个注解与方法在三种 Broker 上运行。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    public static class Listener {
        private final List<Observation> observations = new CopyOnWriteArrayList<>();

        @EventSubscription(id = "contract-listener", binding = "test")
        public EventOutcome onEvent(Payload event, EventDelivery delivery) {
            final var context = delivery.context();
            observations.add(new Observation(context.messageId(), context.retryCount(),
                    TenantContextHolder.getTenantId(), UserContextHolder.getId(),
                    SecurityContextHolder.getContext().getAuthentication().getName(),
                    context.attribute("request.tenantId").getOrElse("missing")));
            return switch (event.action()) {
                case "ack" -> delivery.ack();
                case "reject" -> delivery.reject("unsupported order");
                case "fail" -> throw new IllegalStateException("temporary failure");
                case "restart" -> context.retryCount() == 0 ? delivery.retry(Duration.ofSeconds(2)) : delivery.ack();
                default -> context.retryCount() == 0 ? delivery.retry() : delivery.ack();
            };
        }

        public List<Observation> observations(String id) {
            return observations.stream().filter(observation -> observation.id().equals(id)).toList();
        }
    }
}
