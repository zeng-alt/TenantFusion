package com.github.zeng.alt.security.data.jpa;

import com.github.zeng.alt.security.data.DataPermissionQuery;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 嵌套作用域不能放宽外层授权，受控策略读取及异常均恢复上下文。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class DataPermissionContextTest {
    @Test
    void rejectsDifferentNestedScopesAndRestoresAfterFailure() {
        final var self = new DataPermissionQuery("t1", "7", false, true, Set.of());
        final var all = new DataPermissionQuery("t1", "7", true, false, Set.of());
        try (final var ignored = DataPermissionContext.open(String.class, self)) {
            assertThatThrownBy(() -> DataPermissionContext.open(String.class, all))
                    .hasMessageContaining("nested query");
            assertThat(DataPermissionContext.require(String.class)).isEqualTo(self);
        }
        assertThat(DataPermissionContext.active()).isFalse();
    }

    @Test
    void restoresPolicyReadsWhenNestedOrThrowing() {
        assertThatThrownBy(() -> DataPermissionContext.readPolicy(() -> {
            assertThat(DataPermissionContext.readPolicy(DataPermissionContext::policyRead)).isTrue();
            assertThat(DataPermissionContext.policyRead()).isTrue();
            throw new IllegalStateException("policy unavailable");
        })).hasMessage("policy unavailable");
        assertThat(DataPermissionContext.policyRead()).isFalse();
    }
}
