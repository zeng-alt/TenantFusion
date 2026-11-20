package com.github.zeng.alt.security.abac.spel;

import com.github.zeng.alt.security.abac.AbacContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 统一表达式上下文的回归。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class SpelAbacContextTest {
    private final SpelAbacEvaluator evaluator = new SpelAbacEvaluator();

    @Test
    void readsPrincipalResourceAndEnvironmentThroughProperties() {
        final var context = new AbacContext("t001", "user:update", Map.of("deptId", 10L),
                Map.of("deptId", 10L), Map.of("hour", 9));
        assertThat(evaluator.evaluate("principal.deptId == resource.deptId and env.hour >= 9", context).get()).isTrue();
    }

    @Test
    void readsNestedCollectionsWithoutExposingJavaObjects() {
        final var context = new AbacContext("t001", "read", Map.of("profile", Map.of("deptId", 10L)),
                Map.of("departments", java.util.List.of(10L, 20L)), Map.of());
        assertThat(evaluator.evaluate("principal.profile.deptId == resource.departments[0]", context).get()).isTrue();
    }
}
