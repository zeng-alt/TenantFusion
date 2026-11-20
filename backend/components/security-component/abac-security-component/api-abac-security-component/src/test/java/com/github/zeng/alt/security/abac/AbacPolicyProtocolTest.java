package com.github.zeng.alt.security.abac;

import com.github.zeng.alt.security.abac.event.AbacPolicyChangedEvent;
import com.github.zeng.alt.security.abac.policy.AbacPolicyDto;
import com.github.zeng.alt.security.abac.policy.AbacPolicyQuery;
import com.github.zeng.alt.security.abac.policy.AbacPolicySnapshotDto;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 纯文本协议、不可变集合和 Native 绑定元数据回归。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class AbacPolicyProtocolTest {
    @Test
    void roundTripsPolicyWithoutLeakingExecutionObjects() {
        final AbacPolicy policy = new AbacPolicy("1", "t001", "update", "true", true);
        final AbacPolicyDto dto = AbacPolicyDto.from(policy);
        assertThat(dto.condition()).isEqualTo("true");

        assertThat(dto.toPolicy()).isEqualTo(policy);
    }

    @Test
    void rejectsBlankProtocolKeysAndVersions() {
        assertThatThrownBy(() -> new AbacPolicyQuery(" ", "update")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AbacPolicyQuery("t001", "")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AbacPolicySnapshotDto("t001", "update", "", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void snapshotsAndChangeKeysAreImmutable() {
        final AbacPolicyQuery query = new AbacPolicyQuery("t001", "update");
        final List<AbacPolicyQuery> keys = new ArrayList<>(List.of(query, query));
        final AbacPolicyChangedEvent event = new AbacPolicyChangedEvent(keys);
        keys.clear();
        assertThat(event.keys()).containsExactly(query);
        assertThatThrownBy(() -> event.keys().clear()).isInstanceOf(UnsupportedOperationException.class);
        final AbacPolicySnapshotDto snapshot = new AbacPolicySnapshotDto("t001", "update", "v1", List.of());
        assertThat(snapshot.policies()).isEmpty();
        assertThatThrownBy(() -> snapshot.policies().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void registersNativeProtocolBindings() {
        final RuntimeHints hints = new RuntimeHints();
        new AbacRuntimeHints().registerHints(hints, getClass().getClassLoader());
        assertThat(hints.reflection().getTypeHint(AbacPolicySnapshotDto.class)).isNotNull();
        assertThat(hints.reflection().getTypeHint(AbacPolicyDto.class)).isNotNull();
        assertThat(hints.reflection().getTypeHint(AbacPolicyChangedEvent.class)).isNotNull();
    }
}
