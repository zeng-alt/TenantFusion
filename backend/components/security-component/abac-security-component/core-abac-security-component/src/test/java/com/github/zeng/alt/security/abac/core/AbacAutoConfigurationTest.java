package com.github.zeng.alt.security.abac.core;

import com.github.zeng.alt.security.abac.AbacAuthorizationBuilder;
import com.github.zeng.alt.security.abac.AbacContext;
import com.github.zeng.alt.security.abac.AbacTemplate;
import io.vavr.control.Either;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 自动配置及配置贡献者测试。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class AbacAutoConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AbacAutoConfiguration.class))
            .withBean(AbacEvaluator.class, () -> (expression, context) -> Either.right(true));
    private final AbacContext request = new AbacContext("t001", "update", Map.of(), Map.of(), Map.of());

    @Test
    void shouldFailStartupWithoutPolicyLoader() {
        runner.run(context -> assertThat(context).hasFailed());
    }

    @Test
    void shouldFailStartupWithoutEvaluatorInsteadOfSilentlyDisablingAbac() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(AbacAutoConfiguration.class))
                .withBean(AbacPolicyLoader.class, () -> ignored -> List.of())
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void shouldBindRequiredPermissions() {
        runner.withBean(AbacPolicyLoader.class, () -> ignored -> List.of())
                .withPropertyValues("security.abac.required-permissions[0]=update")
                .run(context -> {
                    assertThat(context).hasSingleBean(AbacTemplate.class);
                    assertThat(context.getBean(AbacTemplate.class).authorize(request).get().getLeft().code())
                            .isEqualTo("POLICY_MISSING");
                });
    }

    @Test
    void shouldApplyCustomizerWithoutReplacingTemplate() {
        runner.withBean(AbacPolicyLoader.class, () -> ignored -> List.of())
                .withBean(AbacTemplateCustomizer.class, () -> properties -> properties.setRequiredPermissions(Set.of("update")))
                .run(context -> assertThat(context.getBean(AbacTemplate.class).authorize(request).get().isLeft()).isTrue());
    }

    @Test
    void shouldBackOffForUserTemplate() {
        final AbacTemplate replacement = request -> new AbacAuthorizationBuilder(required -> io.vavr.control.Either.right(true));
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(AbacAutoConfiguration.class))
                .withBean(AbacTemplate.class, () -> replacement)
                .run(context -> assertThat(context.getBean(AbacTemplate.class)).isSameAs(replacement));
    }
}
