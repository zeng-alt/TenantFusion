package com.github.zeng.alt.event.context;

import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.api.UserContextHolder;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import io.namastack.outbox.Outbox;
import io.namastack.outbox.annotation.OutboxFallbackHandler;
import io.namastack.outbox.annotation.OutboxHandler;
import io.namastack.outbox.handler.OutboxFailureContext;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.modulith.events.Externalized;
import org.springframework.modulith.events.FailedEventPublications;
import org.springframework.modulith.events.ResubmissionOptions;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * 真实 JPA/Modulith/Namastack 重试；所有业务事件均为不携带上下文的普通 record。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@SpringBootTest(classes = EventContextIntegrationTest.Application.class, properties = {
        "spring.main.web-application-type=none",
        "spring.datasource.url=jdbc:h2:mem:transparent_event_context;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.modulith.events.externalization.mode=outbox",
        "namastack.outbox.polling.fixed.interval=50ms",
        "namastack.outbox.retry.policy=fixed",
        "namastack.outbox.retry.fixed.delay=50ms",
        "namastack.outbox.retry.max-retries=2",
        "namastack.outbox.instance.graceful-shutdown-timeout=0s"
})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class EventContextIntegrationTest {

    private final ApplicationEventPublisher events;
    private final JdbcTemplate jdbc;
    private final PlatformTransactionManager transactions;
    private final RecordingHandler handler;
    private final Outbox outbox;

    @AfterEach
    void clearContext() {
        TenantContextHolder.clear();
        UserContextHolder.clear();
    }

    @Test
    void restoresNativeOutboxContextForPayloadOnlyHandlerOnRetry() {
        final String id = "retry-" + UUID.randomUUID();
        TenantContextHolder.setTenantId("origin");
        final var user = SecurityUser.withUsername("alice").id("42").password("secret").build();
        UserContextHolder.set(UsernamePasswordAuthenticationToken.authenticated(user, "token", List.of()));
        new TransactionTemplate(transactions).executeWithoutResult(status -> events.publishEvent(new ExternalEvent(id)));
        clearContext();
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(jdbc.queryForObject(
                "select status from outbox_record where record_key = ?", String.class, id
        )).isEqualTo("COMPLETED"));
        assertThat(handler.deliveries(id)).hasSize(2).allSatisfy(observation -> {
            assertThat(observation.contextByKey()).containsEntry("request.tenantId", "origin");
            assertThat(observation.userId()).isEqualTo("42");
            assertThat(observation.username()).isEqualTo("alice");
        });
        assertThat(jdbc.queryForObject(
                "select context from outbox_record where record_key = ?", String.class, id
        )).contains("request.tenantId", "origin").doesNotContain("secret", "token");
        assertThat(jdbc.queryForObject(
                "select payload from outbox_record where record_key = ?", String.class, id
        )).contains(id).doesNotContain("request.tenantId", "$alt.event.context");
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void restoresOutboxContextForFallbackAfterRetriesAreExhausted(boolean directOutbox) {
        final String id = "fail-" + UUID.randomUUID();
        TenantContextHolder.setTenantId("fallback-tenant");
        UserContextHolder.set(UsernamePasswordAuthenticationToken.authenticated(
                SecurityUser.withUsername("fallback-user").id("43").build(), null, List.of()
        ));
        new TransactionTemplate(transactions).executeWithoutResult(status -> publish(new ExternalEvent(id), directOutbox));
        clearContext();
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(handler.fallbacks(id)).containsExactly("fallback-tenant"));
        assertThat(handler.fallbackUserIds(id)).containsExactly("43");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void rollsBackOutboxWithBusinessTransaction(boolean directOutbox) {
        final String id = UUID.randomUUID().toString();
        jdbc.execute("create table if not exists context_business (id varchar(255) primary key)");
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            jdbc.update("insert into context_business values (?)", id);
            publish(new ExternalEvent(id), directOutbox);
            status.setRollbackOnly();
        });
        assertThat(jdbc.queryForObject(
                "select count(*) from outbox_record where record_key = ?", Integer.class, id
        )).isZero();
        assertThat(jdbc.queryForObject(
                "select count(*) from context_business where id = ?", Integer.class, id
        )).isZero();
    }

    @Test
    void restoresModulithContextAfterDatabaseReload(@Autowired FailedEventPublications publications) {
        final String id = UUID.randomUUID().toString();
        TenantContextHolder.setTenantId("modulith-tenant");
        UserContextHolder.set(UsernamePasswordAuthenticationToken.authenticated(
                SecurityUser.withUsername("modulith-user").id("44").build(), null, List.of()
        ));
        new TransactionTemplate(transactions).executeWithoutResult(status -> events.publishEvent(new LocalEvent(id)));
        clearContext();
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(jdbc.queryForObject(
                "select count(*) from event_publication where status = 'FAILED' and serialized_event like ?",
                Integer.class, "%" + id + "%"
        )).isEqualTo(1));
        assertThat(jdbc.queryForObject(
                "select serialized_event from event_publication where serialized_event like ?",
                String.class, "%" + id + "%"
        ))
                .contains("$alt.event.context", "request.tenantId", "modulith-tenant");
        publications.resubmit(ResubmissionOptions.defaults());
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(handler.localTenants(id)).containsExactly("modulith-tenant", "modulith-tenant");
            assertThat(handler.localUserIds(id)).containsExactly("44", "44");
            assertThat(jdbc.queryForObject(
                    "select count(*) from event_publication where status = 'COMPLETED' and serialized_event like ?",
                    Integer.class, "%" + id + "%"
            )).isEqualTo(1);
        });
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    void replaysLegacyDatabasePublicationWithoutEventContext(@Autowired FailedEventPublications publications) {
        final String id = UUID.randomUUID().toString();
        TenantContextHolder.setTenantId("legacy-origin");
        new TransactionTemplate(transactions).executeWithoutResult(status -> events.publishEvent(new LocalEvent(id)));
        clearContext();
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(jdbc.queryForObject(
                "select count(*) from event_publication where status = 'FAILED' and serialized_event like ?",
                Integer.class, "%" + id + "%"
        )).isEqualTo(1));
        jdbc.update("update event_publication set serialized_event = ? where serialized_event like ?",
                "{\"id\":\"" + id + "\"}", "%" + id + "%");
        publications.resubmit(ResubmissionOptions.defaults());
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(handler.localTenants(id)).containsExactly("legacy-origin", null);
            assertThat(jdbc.queryForObject(
                    "select count(*) from event_publication where status = 'COMPLETED' and serialized_event like ?",
                    Integer.class, "%" + id + "%"
            )).isEqualTo(1);
        });
    }

    @Test
    void preservesCallerContextForOrdinarySpringEvent() {
        TenantContextHolder.setTenantId("caller");
        events.publishEvent(new PlainEvent("plain"));
        assertThat(handler.localTenants("plain")).containsExactly("caller");
        assertThat(TenantContextHolder.getTenantId()).isEqualTo("caller");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    void capturesAllDirectOutboxOverloadsWithoutSpringPublication(int overload) {
        final String id = "direct-" + UUID.randomUUID();
        TenantContextHolder.setTenantId("direct-tenant");
        UserContextHolder.set(UsernamePasswordAuthenticationToken.authenticated(
                SecurityUser.withUsername("direct-user").id("45").build(), null, List.of()
        ));
        final var event = new DirectEvent(id);
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            switch (overload) {
                case 0 -> outbox.schedule(event);
                case 1 -> outbox.schedule(event, id);
                case 2 -> outbox.schedule(event, Map.of("business.source", "direct"));
                case 3 -> outbox.schedule(event, id, Map.of("business.source", "direct"));
                default -> throw new IllegalArgumentException("unknown overload");
            }
        });
        clearContext();
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(jdbc.queryForObject(
                "select status from outbox_record where payload like ?", String.class, "%" + id + "%"
        )).isEqualTo("COMPLETED"));
        assertThat(handler.deliveries(id)).hasSize(2).allSatisfy(observation -> {
            assertThat(observation.contextByKey()).containsEntry("request.tenantId", "direct-tenant");
            assertThat(observation.userId()).isEqualTo("45");
            assertThat(observation.username()).isEqualTo("direct-user");
        });
        if (overload >= 2) {
            assertThat(jdbc.queryForObject(
                    "select context from outbox_record where payload like ?", String.class, "%" + id + "%"
            )).contains("business.source", "direct");
        }
        assertThat(jdbc.queryForObject(
                "select count(*) from event_publication where serialized_event like ?", Integer.class, "%" + id + "%"
        )).isZero();
    }

    @Test
    void retainsPublicationContextWhenTransactionChangesItBeforeCommit() {
        final String id = UUID.randomUUID().toString();
        TenantContextHolder.setTenantId("at-publication");
        UserContextHolder.set(UsernamePasswordAuthenticationToken.authenticated(
                SecurityUser.withUsername("publisher").id("46").build(), null, List.of()
        ));
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            events.publishEvent(new BeforeCommitEvent(id));
            TenantContextHolder.setTenantId("at-commit");
            UserContextHolder.clear();
        });
        assertThat(handler.localTenants(id)).containsExactly("at-publication");
        assertThat(handler.localUserIds(id)).containsExactly("46");
        assertThat(TenantContextHolder.getTenantId()).isEqualTo("at-commit");
        assertThat(UserContextHolder.getId()).isNull();
    }

    private void publish(ExternalEvent event, boolean directOutbox) {
        if (directOutbox) {
            outbox.schedule(event, event.id());
            return;
        }
        events.publishEvent(event);
    }

    /**
     * 最小独立应用。
     *
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class Application {

        @Bean
        RecordingHandler recordingHandler(EventContextTemplate contexts) {
            return new RecordingHandler(contexts);
        }
    }

    /**
     * Handler 不声明上下文接口或 metadata 参数。
     *
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    @RequiredArgsConstructor
    public static class RecordingHandler {

        private final EventContextTemplate contexts;
        private final List<Observation> observations = new CopyOnWriteArrayList<>();
        private final List<Observation> fallbacks = new CopyOnWriteArrayList<>();
        private final List<Observation> locals = new CopyOnWriteArrayList<>();

        @OutboxHandler
        public void deliver(ExternalEvent event) {
            observations.add(observe(event.id()));
            if (event.id().startsWith("fail-") || deliveries(event.id()).size() == 1) {
                throw new IllegalStateException("delivery failed");
            }
        }

        @OutboxHandler
        public void deliverDirect(DirectEvent event) {
            observations.add(observe(event.id()));
            if (deliveries(event.id()).size() == 1) {
                throw new IllegalStateException("retry direct event");
            }
        }

        @OutboxFallbackHandler
        public void fallback(ExternalEvent event, OutboxFailureContext failure) {
            fallbacks.add(observe(event.id()));
        }

        @ApplicationModuleListener
        public void onLocalEvent(LocalEvent event) {
            locals.add(observe(event.id()));
            if (localTenants(event.id()).size() == 1) {
                throw new IllegalStateException("retry local event");
            }
        }

        @EventListener
        public void onPlainEvent(PlainEvent event) {
            locals.add(observe(event.id()));
            TenantContextHolder.setTenantId("changed-by-listener");
        }

        @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
        public void onBeforeCommitEvent(BeforeCommitEvent event) {
            locals.add(observe(event.id()));
        }

        private Observation observe(String id) {
            return new Observation(id, contexts.capture(), UserContextHolder.getId(), UserContextHolder.getUsername());
        }

        List<Observation> deliveries(String id) {
            return observations.stream().filter(o -> o.id().equals(id)).toList();
        }

        List<String> localUserIds(String id) {
            return locals.stream().filter(o -> o.id().equals(id)).map(Observation::userId).toList();
        }

        List<String> fallbackUserIds(String id) {
            return fallbacks.stream().filter(o -> o.id().equals(id)).map(Observation::userId).toList();
        }

        List<String> localTenants(String id) {
            return locals.stream().filter(o -> o.id().equals(id))
                    .map(o -> o.contextByKey().get("request.tenantId")).toList();
        }

        List<String> fallbacks(String id) {
            return fallbacks.stream().filter(o -> o.id().equals(id))
                    .map(o -> o.contextByKey().get("request.tenantId")).toList();
        }
    }

    /**
     * 普通外部化事件。
     *
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    @Externalized("context.events::#{id}")
    public record ExternalEvent(String id) {
    }

    /**
     * 普通本地持久化事件。
     *
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    public record LocalEvent(String id) {
    }

    /**
     * 普通 Spring 事件。
     *
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    public record PlainEvent(String id) {
    }

    /**
     * 仅通过 Outbox 发布，没有外部化注解。
     *
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    public record DirectEvent(String id) {
    }

    /**
     * 不触发 Modulith 持久化的提交前事件。
     *
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    public record BeforeCommitEvent(String id) {
    }

    /**
     * 测试观测记录。
     *
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    public record Observation(String id, Map<String, String> contextByKey, String userId, String username) {
    }
}
