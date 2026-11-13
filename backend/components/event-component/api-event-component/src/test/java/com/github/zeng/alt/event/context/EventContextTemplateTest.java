package com.github.zeng.alt.event.context;

import com.github.zeng.alt.tenant.api.TenantContextHolder;
import com.github.zeng.alt.tenant.api.TenantRouting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证上下文 Bean 扩展及执行作用域，无需业务事件继承。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class EventContextTemplateTest {

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void decoratesTaskAndRestoresOriginalTenantOnFailure() {
        final var factory = new DefaultListableBeanFactory();
        factory.registerSingleton("requestContext", new RequestEventContextHandler());
        final var contexts = new EventContextTemplate(factory.getBeanProvider(EventContextHandler.class));
        TenantContextHolder.setTenantId("origin");
        TenantContextHolder.setRouting(new TenantRouting("origin", "db", "schema", "suffix", true));
        final Runnable task = contexts.decorate(() -> {
            assertThat(TenantContextHolder.getTenantId()).isEqualTo("origin");
            assertThat(TenantContextHolder.getRouting().dataSourceKey()).isEqualTo("db");
            throw new IllegalStateException("failed");
        });
        TenantContextHolder.clear();
        TenantContextHolder.setTenantId("worker");
        assertThatThrownBy(task::run).hasMessage("failed");
        assertThat(TenantContextHolder.getTenantId()).isEqualTo("worker");
        assertThat(TenantContextHolder.getRouting()).isNull();
    }

    @Test
    void restoresNestedCustomContextThroughContributedHandler() {
        final var value = new ThreadLocal<String>();
        final var factory = new DefaultListableBeanFactory();
        factory.registerSingleton("customContext", new EventContextHandler() {
            @Override
            public String key() {
                return "custom";
            }

            @Override
            public Map<String, String> capture() {
                return value.get() == null ? Map.of() : Map.of("id", value.get());
            }

            @Override
            public Scope restore(Map<String, String> contextByKey) {
                final String previous = value.get();
                value.set(contextByKey.get("id"));
                return () -> value.set(previous);
            }
        });
        final var contexts = new EventContextTemplate(factory.getBeanProvider(EventContextHandler.class));
        value.set("caller");
        assertThat(contexts.capture()).containsEntry("custom.id", "caller");
        try (final var outer = contexts.restore(Map.of("custom.id", "outer"))) {
            try (final var inner = contexts.restore(Map.of())) {
                assertThat(value.get()).isNull();
            }
            assertThat(value.get()).isEqualTo("outer");
        }
        assertThat(value.get()).isEqualTo("caller");
        value.remove();
    }
}
