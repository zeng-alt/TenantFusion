package com.github.zeng.alt.security.abac.core;

import com.github.zeng.alt.security.abac.AbacContext;
import com.github.zeng.alt.security.abac.AbacFailure;
import com.github.zeng.alt.security.abac.AbacPolicy;
import com.github.zeng.alt.security.abac.AbacTemplate;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 写入契约可用纯内存存储实现并驱动授权，不依赖 JPA、JDBC 或任何具体持久化技术。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class AbacPolicyStoreTest {

    private final Map<String, AbacPolicy> policyById = new HashMap<>();
    private final AtomicLong ids = new AtomicLong();
    private final AbacPolicyStore store = new AbacPolicyStore() {
        @Override
        public Either<AbacFailure, AbacPolicy> create(AbacPolicyCreateCmd cmd) {
            final var policy = new AbacPolicy(Long.toString(ids.incrementAndGet()), cmd.tenant(), cmd.permission(),
                    cmd.condition(), cmd.preAuth(), cmd.resourceId());
            policyById.put(policy.id(), policy);
            return Either.right(policy);
        }

        @Override
        public Either<AbacFailure, AbacPolicy> update(AbacPolicy policy) {
            if (!policyById.containsKey(policy.id())) {
                return Either.left(new AbacFailure("POLICY_NOT_FOUND", "策略不存在"));
            }
            policyById.put(policy.id(), policy);
            return Either.right(policy);
        }

        @Override
        public Either<AbacFailure, Boolean> delete(String tenant, String id) {
            final var removed = Option.of(policyById.get(id)).filter(policy -> policy.tenant().equals(tenant));
            removed.forEach(policy -> policyById.remove(policy.id()));
            return removed.map(policy -> true).toEither(new AbacFailure("POLICY_NOT_FOUND", "策略不存在"));
        }
    };
    private final AbacContext context = new AbacContext(
            "t001", "user:update", Map.of("deptId", 10L), Map.of("deptId", 10L), Map.of());
    private final AbacTemplate template = new AbacTemplateImpl(
            request -> policyById.values().stream()
                    .filter(policy -> policy.tenant().equals(request.tenant())
                            && policy.permission().equals(request.permission()))
                    .toList(),
            (condition, request) -> Either.right("true".equals(condition)),
            new AbacProperties());
    private final AbacPolicyCreateCmd cmd = new AbacPolicyCreateCmd("t001", "user:update", "true", true, Option.none());

    @Test
    void shouldCreatePersistedPolicyAndAuthorizeWithIt() {
        final var created = store.create(cmd).get();
        assertThat(cmd).isEqualTo(new AbacPolicyCreateCmd("t001", "user:update", "true", true, Option.none()));
        assertThat(created.id()).isEqualTo("1");
        assertThat(created.tenant()).isEqualTo("t001");
        assertThat(created.permission()).isEqualTo("user:update");
        assertThat(created.condition()).isEqualTo("true");
        assertThat(created.preAuth()).isTrue();
        assertThat(created.resourceId()).isEmpty();
        assertThat(template.authorize(context).get().isRight()).isTrue();
    }

    @Test
    void shouldFlipAuthorizationAfterUpdatingCondition() {
        final var created = store.create(cmd).get();
        final var updated = new AbacPolicy(created.id(), "t001", "user:update", "false", true, Option.none());
        assertThat(store.update(updated).get()).isEqualTo(updated);
        assertThat(template.authorize(context).get().getLeft().code()).isEqualTo("POLICY_DENIED");
    }

    @Test
    void shouldDeletePolicyAndFallBackToRequiredPolicySemantics() {
        final var created = store.create(cmd).get();
        assertThat(store.delete("t001", created.id()).get()).isTrue();
        assertThat(template.authorize(context).get().isRight()).isTrue();
        assertThat(template.authorize(context).requirePolicy().get().getLeft().code()).isEqualTo("POLICY_MISSING");
    }

    @Test
    void shouldReportMissingPolicyOnUpdateAndDelete() {
        final var missing = new AbacPolicy("404", "t001", "user:update", "true", true, Option.none());
        assertThat(store.update(missing).getLeft().code()).isEqualTo("POLICY_NOT_FOUND");
        assertThat(store.delete("t001", "404").getLeft().code()).isEqualTo("POLICY_NOT_FOUND");
        assertThat(store.delete("t002", "404").getLeft().code()).isEqualTo("POLICY_NOT_FOUND");
    }
}