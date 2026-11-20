package com.github.zeng.alt.security.abac;

import io.vavr.control.Either;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 公开契约独立运行，不需要 Boot、Web、JPA 或具体求值引擎。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class AbacApiTest {
    @Test
    void shouldExposeImmutableContextAndPolicyContracts() {
        final var context = new AbacContext("t001", "update", Map.of("id", "1"), Map.of(), Map.of("action", "PATCH"));
        final var policy = new AbacPolicy("11", "t001", "update", "true", true);
        final var failure = new AbacFailure("DENIED", "无权限");
        assertThat(context.tenant()).isEqualTo(policy.tenant());
        assertThat(context.permission()).isEqualTo("update");
        assertThat(context.principal()).containsEntry("id", "1");
        assertThat(context.resource()).isEmpty();
        assertThat(context.env()).containsEntry("action", "PATCH");
        assertThat(policy.id()).isEqualTo("11");
        assertThat(policy.permission()).isEqualTo("update");
        assertThat(policy.condition()).isEqualTo("true");
        assertThat(policy.preAuth()).isTrue();

        assertThat(failure.code()).isEqualTo("DENIED");
        assertThat(failure.message()).isEqualTo("无权限");
        assertThatThrownBy(() -> context.principal().put("id", "2")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldEvaluateEachTerminalCallAndPropagateRequiredFlag() {
        final var calls = new AtomicInteger();
        final AbacTemplate template = context -> new AbacAuthorizationBuilder(required -> {
            calls.incrementAndGet();
            return required ? Either.left(new AbacFailure("MISSING", "缺少策略")) : Either.right(true);
        });
        final var builder = template.authorize(new AbacContext("t001", "update", Map.of(), Map.of(), Map.of()));
        assertThat(builder.get().get()).isTrue();
        assertThat(builder.requirePolicy()).isSameAs(builder);
        assertThat(builder.get().getLeft().code()).isEqualTo("MISSING");
        assertThat(calls).hasValue(2);
    }

    @Test
    void shouldCopyAllSupportedScalarTypes() {
        List.of("x", true, (byte) 1, (short) 2, 3, 4L, 5F, 6D, BigDecimal.ONE, BigInteger.ONE).forEach(value -> {
            final var source = new HashMap<String, Object>();
            source.put("value", value);
            final var context = new AbacContext("t001", "update", source, Map.of(), Map.of());
            source.clear();
            assertThat(context.principal()).containsEntry("value", value);
        });
    }

    @Test
    void shouldRejectInvalidBoundaryValues() {
        assertThatThrownBy(() -> new AbacContext(" ", "update", Map.of(), Map.of(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AbacContext("t001", "", Map.of(), Map.of(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AbacContext("t001", "update", null, Map.of(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AbacContext("t001", "update", Map.of(" ", true), Map.of(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AbacContext("t001", "update", Map.of("bean", new Object()), Map.of(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldHaveNoRuntimeDependencyOnBootOrImplementations() {
        List.of("org.springframework.boot.SpringApplication", "org.springframework.expression.Expression",
                "jakarta.persistence.EntityManager", "com.github.zeng.alt.security.abac.core.AbacTemplateImpl")
                .forEach(name -> assertThatThrownBy(() -> Class.forName(name)).isInstanceOf(ClassNotFoundException.class));
    }
}
