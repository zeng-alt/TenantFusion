package com.github.zeng.alt.security.abac.server.service;

import com.github.zeng.alt.security.abac.AbacFailure;
import com.github.zeng.alt.security.abac.AbacPolicy;
import com.github.zeng.alt.security.abac.policy.AbacPolicyQuery;
import io.vavr.control.Either;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 权威查询的稳定版本、作用域与错误协议回归。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class AbacPolicyQueryServiceImplTest {
    private final AbacPolicyQuery query = new AbacPolicyQuery("t001", "update");

    @Test
    void revisionIsStableAcrossOrderingAndChangesWithPolicyContent() {
        final AbacPolicy first = policy("1", "true");
        final AbacPolicy second = policy("2", "false");
        final var policies = new AtomicReference<>(List.of(first, second));
        final var service = new AbacPolicyQueryServiceImpl(key -> Either.right(policies.get()));
        final String initial = service.get(query).get().revision();
        policies.set(List.of(second, first));
        assertThat(service.get(query).get().revision()).isEqualTo(initial);
        assertThat(service.get(query).get().policies()).extracting(policy -> policy.id()).containsExactly("2", "1");
        policies.set(List.of(first, policy("2", "true")));
        assertThat(service.get(query).get().revision()).isNotEqualTo(initial);
    }

    @Test
    void successfulEmptyRulesHaveARevisionAndRemainDistinctFromFailure() {
        final var empty = new AbacPolicyQueryServiceImpl(key -> Either.right(List.of())).get(query).get();
        assertThat(empty.policies()).isEmpty();
        assertThat(empty.revision()).hasSize(64);
        final var unavailable = new AbacPolicyQueryServiceImpl(key -> {
            throw new IllegalStateException("database offline");
        });
        assertThat(unavailable.get(query).getLeft().code()).isEqualTo("POLICY_QUERY_FAILED");
    }

    @Test
    void preservesExpectedBoundaryFailure() {
        final var service = new AbacPolicyQueryServiceImpl(key ->
                Either.left(new AbacFailure("TENANT_MISMATCH", "tenant mismatch")));
        assertThat(service.get(query).getLeft().code()).isEqualTo("TENANT_MISMATCH");
    }

    @Test
    void rejectsARepositoryReturningOtherTenantOrPermissionRules() {
        final var wrongTenant = new AbacPolicy("1", "t002", "update", "true", true);
        final var wrongPermission = new AbacPolicy("1", "t001", "delete", "true", true);
        assertThat(new AbacPolicyQueryServiceImpl(key -> Either.right(List.of(wrongTenant)))
                .get(query).getLeft().code()).isEqualTo("POLICY_SCOPE_MISMATCH");
        assertThat(new AbacPolicyQueryServiceImpl(key -> Either.right(List.of(wrongPermission)))
                .get(query).isLeft()).isTrue();
    }

    private AbacPolicy policy(String id, String condition) {
        return new AbacPolicy(id, "t001", "update", condition, true);
    }
}
