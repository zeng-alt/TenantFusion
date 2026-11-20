package com.github.zeng.alt.security.abac.spel;

import com.github.zeng.alt.security.abac.AbacContext;
import com.github.zeng.alt.security.abac.AbacInvocationDto;
import io.vavr.control.Option;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 认证函数、参数变量与可视化构建器的数据操作使用相同求值器。
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class SpelAbacFunctionsTest {
    private final SpelAbacEvaluator evaluator = new SpelAbacEvaluator();
    private final AbacContext context = new AbacContext("t", "update", Map.of("username", "editor"),
            Map.of("name", "hello", "roles", List.of("EDITOR", "AUDITOR")), Map.of("hour", 10))
            .withInvocation(AbacInvocationDto.method(Map.of("authenticated", true, "roles", List.of("EDITOR"),
                    "authorities", List.of("user:update")), Map.of("id", 42L, "dto", Map.of("enabled", true)), true));

    @ParameterizedTest
    @ValueSource(strings = {
            "hasRole('EDITOR') and hasAnyRole('OTHER', 'EDITOR')",
            "hasAuthority('user:update') and hasAnyAuthority('other', 'user:update')",
            "isAuthenticated() and !isAnonymous()",
            "#id == 42 and #dto.enabled and authentication.principal.username == principal.username",
            "resource.name.startsWith('he') and resource.name.endsWith('lo') and resource.name.contains('ell')",
            "resource.name.toUpperCase() == 'HELLO' and resource.name.toLowerCase().length() == 5",
            "resource.roles.contains('AUDITOR') and resource.roles.size() == 2 and !resource.roles.isEmpty()",
            "resource.name.trim().matches('he.*') and resource.name.length == 5",
            "resource.roles.size == 2 and !resource.roles.empty",
            "resource.containsKey('name') and resource.size() == 2 and !resource.isEmpty()"
    })
    void evaluatesSupportedFunctionsAndDataOperations(String condition) {
        assertThat(evaluator.evaluate(condition, context).get()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"principal.getClass()", "T(java.lang.Runtime).getRuntime()", "new java.io.File('/tmp')",
            "@service.delete()", "#dto.enabled = false", "resource.clear()", "principal['class'].forName('X')"})
    void refusesObjectAccessAndMutations(String condition) {
        assertThat(evaluator.prepare(condition).isLeft()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"subject.deptId == 1", "environment.hour == 10", "#unknown == null",
            "principal.unknown == null", "resource['missing'] == null", "hasRole(42)", "isAuthenticated('x')"})
    void rejectsUnknownAttributesOldNamesAndWrongFunctionArguments(String condition) {
        assertThat(evaluator.evaluate(condition, context).isLeft()).isTrue();
    }

    @Test
    void usesPerInvocationArgumentsAndPostReturnObject() {
        assertThat(evaluator.evaluate("hasRole('ADMIN')", context).get()).isFalse();
        final var after = context.withInvocation(context.invocation().after(Option.of(Map.of("id", 42L)), true));
        assertThat(evaluator.evaluate("returnObject.id == #id", after).get()).isTrue();
        assertThat(evaluator.evaluate("returnObject.id == #id", context).isLeft()).isTrue();
        final var other = context.withInvocation(AbacInvocationDto.method(Map.of("authenticated", false), Map.of("id", 43L), false));
        assertThat(evaluator.evaluate("#id == 42", other).get()).isFalse();
        assertThat(evaluator.evaluate("isAnonymous()", other).get()).isTrue();
        assertThat(evaluator.evaluate("hasRole('EDITOR')", other).get()).isFalse();
    }
}
