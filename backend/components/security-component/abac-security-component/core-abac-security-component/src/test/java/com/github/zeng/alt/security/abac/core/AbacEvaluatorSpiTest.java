package com.github.zeng.alt.security.abac.core;

import com.github.zeng.alt.security.abac.AbacContext;
import com.github.zeng.alt.security.abac.AbacFailure;
import com.github.zeng.alt.security.abac.AbacPolicy;
import io.vavr.control.Either;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 核心引擎只依赖求值 SPI，而非某个具体表达式实现。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class AbacEvaluatorSpiTest {
    private final AbacContext context = new AbacContext("t001", "update", Map.of(), Map.of(), Map.of());
    private final AbacPolicyLoader loader = ignored -> List.of(new AbacPolicy("1", "t001", "update", "custom", true));

    @Test
    void shouldAuthorizeWithCustomEvaluator() {
        final AbacEvaluator evaluator = (expression, request) -> Either.right("custom".equals(expression));
        assertThat(evaluator.prepare("custom").get()).isTrue();
        final var template = new AbacTemplateImpl(loader, evaluator, new AbacProperties());
        assertThat(template.authorize(context).get().isRight()).isTrue();
    }

    @Test
    void shouldPreserveEvaluatorFailure() {
        final var failure = new AbacFailure("CUSTOM_FAILURE", "自定义求值失败");
        final AbacEvaluator evaluator = (expression, request) -> Either.left(failure);
        final var template = new AbacTemplateImpl(loader, evaluator, new AbacProperties());
        assertThat(template.authorize(context).get().getLeft()).isSameAs(failure);
    }

    @Test
    void shouldRunWithoutConcreteSpelImplementation() {
        assertThatThrownBy(() -> Class.forName("com.github.zeng.alt.security.abac.spel.SpelAbacEvaluator"))
                .isInstanceOf(ClassNotFoundException.class);
    }
}
