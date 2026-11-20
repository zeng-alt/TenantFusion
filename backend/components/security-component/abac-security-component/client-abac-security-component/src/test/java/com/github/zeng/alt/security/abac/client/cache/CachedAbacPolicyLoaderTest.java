package com.github.zeng.alt.security.abac.client.cache;

import com.github.zeng.alt.security.abac.AbacContext;
import com.github.zeng.alt.security.abac.AbacFailure;
import com.github.zeng.alt.security.abac.client.config.AbacClientProperties;
import com.github.zeng.alt.security.abac.core.AbacEvaluator;
import com.github.zeng.alt.security.abac.core.AbacProperties;
import com.github.zeng.alt.security.abac.core.AbacTemplateImpl;
import com.github.zeng.alt.security.abac.policy.AbacPolicyDto;
import com.github.zeng.alt.security.abac.policy.AbacPolicyQuery;
import com.github.zeng.alt.security.abac.policy.AbacPolicySnapshotDto;
import com.github.zeng.alt.security.abac.spel.SpelAbacEvaluator;
import com.google.common.base.Ticker;
import io.vavr.control.Either;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 真实 SpEL 与本地缓存组合，覆盖缓存命中、预解析、隔离、失效和失败关闭。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class CachedAbacPolicyLoaderTest {
    private final AbacPolicyQuery query = new AbacPolicyQuery("t001", "update");
    private final Map<AbacPolicyQuery, Either<AbacFailure, AbacPolicySnapshotDto>> replies = new HashMap<>();
    private final List<AbacPolicyQuery> requests = new ArrayList<>();
    private final AtomicInteger preparations = new AtomicInteger();
    private final AbacEvaluator evaluator = new AbacEvaluator() {
        private final SpelAbacEvaluator spel = new SpelAbacEvaluator();

        @Override
        public Either<AbacFailure, Boolean> prepare(String condition) {
            preparations.incrementAndGet();
            return spel.prepare(condition);
        }

        @Override
        public Either<AbacFailure, Boolean> evaluate(String condition, AbacContext context) {
            return spel.evaluate(condition, context);
        }
    };
    private final AbacClientProperties properties = new AbacClientProperties();
    private final CachedAbacPolicyLoader loader = loader(Ticker.systemTicker());

    @Test
    void preparesOnLoadAndDoesNotQueryOrPrepareOnCacheHit() {
        assertThat(loader.get(query).isRight()).isTrue();
        assertThat(preparations).hasValue(1);
        assertThat(loader.get(query).isRight()).isTrue();
        assertThat(requests).containsExactly(query);
        assertThat(preparations).hasValue(1);
    }

    @Test
    void reusesExpressionsButEvaluatesCurrentAttributesEveryTime() {
        replies.put(query, Either.right(snapshot(query, "principal['deptId'] == resource['deptId']")));
        final var template = new AbacTemplateImpl(loader, evaluator, new AbacProperties());

        assertThat(template.authorize(context(10L)).get().isRight()).isTrue();
        assertThat(template.authorize(context(20L)).get().getLeft().code()).isEqualTo("POLICY_DENIED");
        assertThat(requests).containsExactly(query);
        assertThat(preparations).hasValue(1);
    }

    @Test
    void isolatesTenantsAndPermissions() {
        final AbacPolicyQuery otherTenant = new AbacPolicyQuery("t002", "update");
        final AbacPolicyQuery otherPermission = new AbacPolicyQuery("t001", "delete");
        assertThat(loader.get(query).isRight()).isTrue();
        assertThat(loader.get(otherTenant).isRight()).isTrue();
        assertThat(loader.get(otherPermission).isRight()).isTrue();
        assertThat(requests).containsExactly(query, otherTenant, otherPermission);
    }

    @Test
    void cachesSuccessfulEmptyRulesWithoutPreparingAnExpression() {
        replies.put(query, Either.right(new AbacPolicySnapshotDto("t001", "update", "empty", List.of())));
        assertThat(loader.get(query).get().policies()).isEmpty();
        assertThat(loader.get(query).get().policies()).isEmpty();
        assertThat(requests).containsExactly(query);
        assertThat(preparations).hasValue(0);
    }

    @Test
    void doesNotCacheSourceFailureAsEmptyPolicies() {
        replies.put(query, Either.left(new AbacFailure("UNAVAILABLE", "server unavailable")));
        assertThat(loader.get(query).getLeft().code()).isEqualTo("UNAVAILABLE");
        replies.put(query, Either.right(snapshot(query, "true")));
        assertThat(loader.get(query).isRight()).isTrue();
        assertThat(requests).containsExactly(query, query);
    }

    @Test
    void unexpectedSourceFailureDeniesAuthorization() {
        final var broken = new CachedAbacPolicyLoader(key -> { throw new IllegalStateException("offline"); },
                evaluator, properties);
        final var template = new AbacTemplateImpl(broken, evaluator, new AbacProperties());
        assertThat(template.authorize(context(10L)).get().getLeft().code()).isEqualTo("POLICY_LOAD_FAILED");
    }

    @Test
    void rejectsSnapshotsAndPoliciesFromOtherScopesBeforePreparation() {
        replies.put(query, Either.right(snapshot(new AbacPolicyQuery("t002", "update"), "true")));
        assertThat(loader.get(query).getLeft().code()).isEqualTo("POLICY_SCOPE_MISMATCH");
        replies.put(query, Either.right(snapshot(new AbacPolicyQuery("t001", "delete"), "true")));
        assertThat(loader.get(query).getLeft().code()).isEqualTo("POLICY_SCOPE_MISMATCH");
        replies.put(query, Either.right(new AbacPolicySnapshotDto("t001", "update", "v1",
                snapshot(new AbacPolicyQuery("t002", "update"), "true").policies())));
        assertThat(loader.get(query).getLeft().code()).isEqualTo("POLICY_SCOPE_MISMATCH");
        assertThat(preparations).hasValue(0);
    }

    @Test
    void invalidExpressionsNeverEnterThePolicyCache() {
        replies.put(query, Either.right(snapshot(query, "T(java.lang.Runtime).getRuntime()")));
        assertThat(loader.get(query).isLeft()).isTrue();
        replies.put(query, Either.right(snapshot(query, "true")));
        assertThat(loader.get(query).isRight()).isTrue();
        assertThat(requests).containsExactly(query, query);
    }

    @Test
    void refreshesChangedRulesBeforeTheNextAuthorization() {
        final var template = new AbacTemplateImpl(loader, evaluator, new AbacProperties());
        assertThat(template.authorize(context(10L)).get().isRight()).isTrue();
        replies.put(query, Either.right(snapshot(query, "false")));
        assertThat(loader.refresh(List.of(query)).get()).isTrue();
        assertThat(template.authorize(context(10L)).get().getLeft().code()).isEqualTo("POLICY_DENIED");
        assertThat(requests).containsExactly(query, query);
        assertThat(preparations).hasValue(2);
    }

    @Test
    void failedRefreshRemovesOldPermits() {
        assertThat(loader.get(query).isRight()).isTrue();
        replies.put(query, Either.left(new AbacFailure("OFFLINE", "offline")));
        assertThat(loader.refresh(List.of(query)).isLeft()).isTrue();
        assertThat(loader.get(query).isLeft()).isTrue();
        assertThat(requests).containsExactly(query, query, query);
    }

    @Test
    void deletedLastRulePreservesRequiredPolicyBehavior() {
        final var template = new AbacTemplateImpl(loader, evaluator, new AbacProperties());
        assertThat(template.authorize(context(10L)).requirePolicy().get().isRight()).isTrue();
        replies.put(query, Either.right(new AbacPolicySnapshotDto("t001", "update", "empty", List.of())));
        loader.refresh(List.of(query));
        assertThat(template.authorize(context(10L)).requirePolicy().get().getLeft().code()).isEqualTo("POLICY_MISSING");
    }

    @Test
    void expiryRequeriesWithoutDependingOnWallClockSleeps() {
        final AtomicLong clock = new AtomicLong();
        properties.setCacheTtl(Duration.ofSeconds(1));
        final CachedAbacPolicyLoader expiring = loader(new Ticker() {
            @Override
            public long read() {
                return clock.get();
            }
        });
        assertThat(expiring.get(query).isRight()).isTrue();
        clock.set(Duration.ofSeconds(2).toNanos());
        assertThat(expiring.get(query).isRight()).isTrue();
        assertThat(requests).containsExactly(query, query);
    }

    private CachedAbacPolicyLoader loader(Ticker ticker) {
        return new CachedAbacPolicyLoader(key -> {
            requests.add(key);
            return replies.getOrDefault(key, Either.right(snapshot(key, "true")));
        }, evaluator, properties, ticker);
    }

    private AbacPolicySnapshotDto snapshot(AbacPolicyQuery key, String condition) {
        return new AbacPolicySnapshotDto(key.tenant(), key.permission(), "revision-" + condition,
                List.of(new AbacPolicyDto("1", key.tenant(), key.permission(), condition, true)));
    }

    private AbacContext context(long resourceDepartment) {
        return new AbacContext("t001", "update", Map.of("deptId", 10L),
                Map.of("deptId", resourceDepartment), Map.of());
    }
}
