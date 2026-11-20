package com.github.zeng.alt.security.abac.server.service;

import com.github.zeng.alt.security.abac.AbacFailure;
import com.github.zeng.alt.security.abac.AbacPolicy;
import com.github.zeng.alt.security.abac.event.AbacPolicyChangedEvent;
import com.github.zeng.alt.security.abac.policy.AbacPolicyQuery;
import com.github.zeng.alt.security.abac.server.AbacPolicyCreateCmd;
import com.github.zeng.alt.security.abac.server.spi.AbacPolicyStore;
import com.github.zeng.alt.security.abac.spel.SpelAbacEvaluator;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 写入前表达式校验、成功发布以及跨权限修改和删除的失效范围回归。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class AbacPolicyServiceImplTest {
    private final AbacPolicyStore store = mock(AbacPolicyStore.class);
    private final List<Object> events = new ArrayList<>();
    private final AbacPolicyServiceImpl service = new AbacPolicyServiceImpl(store, new SpelAbacEvaluator(), events::add);
    private final AbacPolicy policy = new AbacPolicy("1", "t001", "update", "true", true);

    @Test
    void validatesBeforeCreatingAndPublishesTheAffectedKey() {
        final var cmd = new AbacPolicyCreateCmd("t001", "update", "true", true);
        when(store.create(cmd)).thenReturn(Either.right(policy));
        assertThat(service.create(cmd).get()).isEqualTo(policy);
        assertThat(events).containsExactly(new AbacPolicyChangedEvent(List.of(new AbacPolicyQuery("t001", "update"))));
    }

    @Test
    void invalidExpressionDoesNotReachStorage() {
        final var cmd = new AbacPolicyCreateCmd("t001", "update", "T(java.lang.Runtime).getRuntime()", true);
        assertThat(service.create(cmd).isLeft()).isTrue();
        assertThat(service.update(new AbacPolicy("1", "t001", "update", "true +", true))
                .isLeft()).isTrue();
        verifyNoInteractions(store);
        assertThat(events).isEmpty();
    }

    @Test
    void unsuccessfulWriteDoesNotPublish() {
        final var cmd = new AbacPolicyCreateCmd("t001", "update", "true", true);
        when(store.create(cmd)).thenReturn(Either.left(new AbacFailure("REJECTED", "rejected")));
        assertThat(service.create(cmd).isLeft()).isTrue();
        assertThat(events).isEmpty();
    }

    @Test
    void movingPolicyRefreshesBothOldAndNewPermission() {
        final var changed = new AbacPolicy("1", "t001", "delete", "false", true);
        when(store.findById("t001", "1")).thenReturn(Option.of(policy));
        when(store.update(changed)).thenReturn(Either.right(changed));
        assertThat(service.update(changed).get()).isEqualTo(changed);
        assertThat(events).containsExactly(new AbacPolicyChangedEvent(List.of(
                new AbacPolicyQuery("t001", "update"), new AbacPolicyQuery("t001", "delete"))));
    }

    @Test
    void missingPolicyDoesNotUpdateOrPublish() {
        when(store.findById("t001", "1")).thenReturn(Option.none());
        assertThat(service.update(policy).getLeft().code()).isEqualTo("POLICY_NOT_FOUND");
        assertThat(service.delete("t001", "1").getLeft().code()).isEqualTo("POLICY_NOT_FOUND");
        assertThat(events).isEmpty();
    }

    @Test
    void successfulDeletePublishesAndUnchangedDeleteDoesNot() {
        when(store.findById("t001", "1")).thenReturn(Option.of(policy));
        when(store.delete("t001", "1")).thenReturn(Either.right(false), Either.right(true));
        assertThat(service.delete("t001", "1").get()).isFalse();
        assertThat(events).isEmpty();
        assertThat(service.delete("t001", "1").get()).isTrue();
        assertThat(events).containsExactly(new AbacPolicyChangedEvent(List.of(new AbacPolicyQuery("t001", "update"))));
    }
}
