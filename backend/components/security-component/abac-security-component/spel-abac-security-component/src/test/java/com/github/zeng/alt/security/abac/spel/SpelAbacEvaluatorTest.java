package com.github.zeng.alt.security.abac.spel;

import com.github.zeng.alt.security.abac.AbacContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 受限表达式（全局根变量，无 #）、属性快照与解析缓存测试。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class SpelAbacEvaluatorTest {

    private final SpelAbacEvaluator evaluator = new SpelAbacEvaluator();
    private final AbacContext context = new AbacContext(
            "t001", "user:update", Map.of("deptId", 10L),
            Map.of("deptId", 10L), Map.of("action", "PATCH")
    );

    @ParameterizedTest
    @ValueSource(strings = {
            "subject['deptId'] == resource['deptId']",
            "environment['action'] == 'PATCH' and subject['deptId'] >= 10",
            "!(subject['deptId'] < 10) or false", "true",
            "tenant == 't001'", "permission == 'user:update'"
    })
    void shouldAllowReadOnlyScalarComparisons(String expression) {
        assertThat(evaluator.evaluate(expression, context).get()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "T(java.lang.Runtime).getRuntime()", "new java.lang.String('x')", "@someBean.run()",
            "subject['deptId'] = 20", "subject.get('deptId')", "subject.class", "unknown['x']",
            "subject['missing'] == resource['missing']", "true or subject['missing'] == null",
            "subject['deptId'].toString()", "'true'", "1", "null", "true +", "", "'x' matches '.*'",
            "#subject['deptId'] == 10", "#id == 10", "subject", "resource['deptId'] == subject"
    })
    void shouldRejectUnsafeInvalidMissingOrNonBooleanExpressions(String expression) {
        assertThat(evaluator.evaluate(expression, context).isLeft()).isTrue();
    }

    @Test
    void shouldReturnFalseWithoutCoercion() {
        assertThat(evaluator.evaluate("subject['deptId'] != resource['deptId']", context).get()).isFalse();
    }

    @Test
    void shouldRejectBeansAndKeepSnapshotsImmutable() {
        assertThatThrownBy(() -> new AbacContext("t001", "update", Map.of("user", new Object()), Map.of(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> context.subject().put("deptId", 20L)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldParseConditionOnceAndReuseAcrossContexts() {
        final SpelExpressionParser parser = spy(new SpelExpressionParser());
        final SpelAbacEvaluator cached = new SpelAbacEvaluator(parser);
        final String condition = "subject['deptId'] == resource['deptId']";
        final AbacContext granted = new AbacContext(
                "t001", "user:update", Map.of("deptId", 10L), Map.of("deptId", 10L), Map.of());
        final AbacContext denied = new AbacContext(
                "t001", "user:update", Map.of("deptId", 10L), Map.of("deptId", 20L), Map.of());

        assertThat(cached.evaluate(condition, granted).get()).isTrue();
        assertThat(cached.evaluate(condition, denied).get()).isFalse();
        verify(parser, times(1)).parseRaw(condition);
    }

    @Test
    void shouldCheckAttributeExistencePerCallEvenWhenParsedExpressionIsCached() {
        final SpelExpressionParser parser = spy(new SpelExpressionParser());
        final SpelAbacEvaluator cached = new SpelAbacEvaluator(parser);
        final String condition = "subject['deptId'] == 10";
        final AbacContext missing = new AbacContext("t001", "user:update", Map.of(), Map.of(), Map.of());

        assertThat(cached.evaluate(condition, context).get()).isTrue();
        assertThat(cached.evaluate(condition, missing).getLeft().code()).isEqualTo("INVALID_ATTRIBUTES");
        verify(parser, times(1)).parseRaw(condition);
    }

    @Test
    void shouldNotCacheParseFailures() {
        final SpelExpressionParser parser = spy(new SpelExpressionParser());
        final SpelAbacEvaluator cached = new SpelAbacEvaluator(parser);
        final String condition = "true +";

        assertThat(cached.evaluate(condition, context).getLeft().code()).isEqualTo("INVALID_EXPRESSION");
        assertThat(cached.evaluate(condition, context).getLeft().code()).isEqualTo("INVALID_EXPRESSION");
        verify(parser, times(2)).parseRaw(condition);
    }

    @Test
    void shouldCacheStructuralValidationFailure() {
        final SpelExpressionParser parser = spy(new SpelExpressionParser());
        final SpelAbacEvaluator cached = new SpelAbacEvaluator(parser);
        final String condition = "subject.get('deptId')";

        assertThat(cached.evaluate(condition, context).getLeft().code()).isEqualTo("INVALID_ATTRIBUTES");
        assertThat(cached.evaluate(condition, context).getLeft().code()).isEqualTo("INVALID_ATTRIBUTES");
        verify(parser, times(1)).parseRaw(condition);
    }
}