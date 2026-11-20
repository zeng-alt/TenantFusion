package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.admin.query.api.dto.AbacSimulationCmd;
import com.github.zeng.alt.security.abac.method.AbacFieldDto;
import com.github.zeng.alt.security.abac.method.AbacMethodResolver;
import com.github.zeng.alt.security.abac.method.AbacMethodSchemaDto;
import com.github.zeng.alt.security.abac.method.AbacParameterDto;
import com.github.zeng.alt.security.abac.method.AbacSchemaDto;
import com.github.zeng.alt.security.abac.spel.SpelAbacEvaluator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 试算调用运行时求值器，参数位置别名与方法绑定保持一致。
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class AbacWorkbenchTemplateTest {
    private final AbacMethodResolver methods = mock(AbacMethodResolver.class);
    private final AbacWorkbenchTemplate template = new AbacWorkbenchTemplate(methods, new SpelAbacEvaluator());

    @Test
    void bindsParameterAliasesAndProjectsPostResult() {
        schema();
        final var command = new AbacSimulationCmd("user:update",
                "returnObject == #id and #p0 == #a0",
                false,
                "Users.update",
                Map.of("variables", Map.of("id", 42L), "returnObject", 42L));
        assertThat(template.simulate("t001", command).allowed()).isTrue();
    }

    @Test
    void refusesUnknownReferencesAndMalformedContextWithoutRunningBusinessCode() {
        schema();
        final var unknown = new AbacSimulationCmd("user:update", "#missing == 1", true, "Users.update", Map.of());
        assertThat(template.simulate("t001", unknown).code()).isEqualTo("UNKNOWN_ATTRIBUTE");
        final var malformed = new AbacSimulationCmd("user:update",
                "#id == 1",
                true,
                "Users.update",
                Map.of("variables", "not an object"));
        assertThat(template.simulate("t001", malformed).code()).isEqualTo("INVALID_CONTEXT");
    }

    @Test
    void refusesSimulationForAnUnboundMethod() {
        schema();
        final var command = new AbacSimulationCmd("user:update",
                "#id == 1",
                true,
                "Other.update",
                Map.of("variables", Map.of("id", 1)));
        assertThat(template.simulate("t001", command).code()).isEqualTo("METHOD_NOT_FOUND");
    }

    private void schema() {
        final AbacFieldDto id = field("#id");
        final var parameters = List.of(new AbacParameterDto("id", 0, id),
                new AbacParameterDto("p0", 0, field("#p0")), new AbacParameterDto("a0", 0, field("#a0")));
        when(methods.getSchema("user:update")).thenReturn(new AbacSchemaDto("user:update",
                List.of(),
                List.of(new AbacMethodSchemaDto("Users.update", true,
                parameters, field("returnObject"))),
                List.of()));
    }

    private AbacFieldDto field(String path) {
        return new AbacFieldDto(path, "java.lang.Long", true, "测试", List.of());
    }
}
