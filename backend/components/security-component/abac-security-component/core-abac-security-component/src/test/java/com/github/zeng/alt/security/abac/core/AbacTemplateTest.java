package com.github.zeng.alt.security.abac.core;

import com.github.zeng.alt.security.abac.AbacContext;
import com.github.zeng.alt.security.abac.AbacFailure;
import com.github.zeng.alt.security.abac.AbacPolicy;
import com.github.zeng.alt.security.abac.AbacTemplate;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 策略授权行为测试。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class AbacTemplateTest {
    private final AbacEvaluator evaluator = (condition, request) -> switch (condition) {
        case "true" -> Either.right(true);
        case "false" -> Either.right(false);
        default -> Either.left(new AbacFailure("INVALID_EXPRESSION", "测试条件无效"));
    };

    private final AbacContext context = new AbacContext(
            "t001", "user:update", Map.of("deptId", 10L),
            Map.of("id", "42", "deptId", 10L), Map.of("action", "PATCH")
    );

    @Test
    void shouldGrantOnlyWhenEveryPolicyMatches() {
        final var template = template(List.of(policy("true"), policy("true")));
        assertThat(template.authorize(context).get().isRight()).isTrue();
        assertThat(template(List.of(policy("true"), policy("false"))).authorize(context).get().getLeft().code())
                .isEqualTo("POLICY_DENIED");
    }

    @Test
    void shouldDistinguishOptionalAndRequiredMissingPolicies() {
        assertThat(template(List.of()).authorize(context).get().isRight()).isTrue();
        assertThat(template(List.of()).authorize(context).requirePolicy().get().getLeft().code())
                .isEqualTo("POLICY_MISSING");
        final var properties = new AbacProperties();
        properties.setRequiredPermissions(Set.of("user:update"));
        final var configured = new AbacTemplateImpl(ignored -> List.of(), evaluator, properties);
        assertThat(configured.authorize(context).get().getLeft().code()).isEqualTo("POLICY_MISSING");
    }

    @Test
    void shouldRejectLoaderFailureAndForeignTenantPolicies() {
        final AbacPolicyLoader broken = ignored -> { throw new IllegalStateException("database unavailable"); };
        final var template = new AbacTemplateImpl(broken, evaluator, new AbacProperties());
        assertThat(template.authorize(context).get().getLeft().code()).isEqualTo("POLICY_LOAD_FAILED");
        final var foreign = new AbacPolicy("1", "t002", "user:update", "true", true, Option.none());
        assertThat(template(List.of(foreign)).authorize(context).get().getLeft().code()).isEqualTo("TENANT_MISMATCH");
    }

    @Test
    void shouldRejectPostAuthorizationRatherThanSkipIt() {
        final var policy = new AbacPolicy("1", "t001", "user:update", "true", false, Option.none());
        assertThat(template(List.of(policy)).authorize(context).get().getLeft().code())
                .isEqualTo("POST_AUTH_UNSUPPORTED");
    }

    @Test
    void shouldMatchResourceScopeAndRejectMissingResource() {
        final var scoped = new AbacPolicy("1", "t001", "user:update", "true", true, Option.of("42"));
        assertThat(template(List.of(scoped)).authorize(context).get().isRight()).isTrue();
        final var missing = new AbacContext("t001", "user:update", Map.of(), Map.of(), Map.of());
        assertThat(template(List.of(scoped)).authorize(missing).get().getLeft().code()).isEqualTo("RESOURCE_MISSING");
        final var other = new AbacPolicy("2", "t001", "user:update", "true", true, Option.of("99"));
        assertThat(template(List.of(other)).authorize(context).get().getLeft().code()).isEqualTo("POLICY_MISSING");
    }

    @Test
    void shouldStopAfterFirstDeniedPolicy() {
        assertThat(template(List.of(policy("false"), policy("broken("))).authorize(context).get().getLeft().code())
                .isEqualTo("POLICY_DENIED");
    }

    private AbacTemplate template(List<AbacPolicy> policies) {
        return new AbacTemplateImpl(ignored -> policies, evaluator, new AbacProperties());
    }

    private AbacPolicy policy(String expression) {
        return new AbacPolicy("1", "t001", "user:update", expression, true, Option.none());
    }
}
