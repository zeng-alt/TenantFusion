package com.github.zeng.alt.security.abac.spel;

import com.github.zeng.alt.security.abac.AbacContext;
import com.github.zeng.alt.security.abac.AbacPolicy;
import com.github.zeng.alt.security.abac.AbacTemplate;
import com.github.zeng.alt.security.abac.core.AbacEvaluator;
import com.github.zeng.alt.security.abac.core.AbacPolicyLoader;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 从两个 Jar 的 AutoConfiguration.imports 发现配置，验证默认引擎及替换行为。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class SpelAbacAutoConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(Application.class);
    private final AbacContext request = new AbacContext(
            "t001", "update", Map.of("deptId", 10L), Map.of("id", "42", "deptId", 10L), Map.of()
    );

    @Test
    void shouldDiscoverBothConfigurationsAndEvaluatePolicies() {
        runner.withBean(AbacPolicyLoader.class, () -> context -> List.of(new AbacPolicy(
                "1", "t001", "update", "subject['deptId'] == resource['deptId']", true, Option.of("42")
        ))).run(context -> {
            assertThat(context).hasSingleBean(AbacTemplate.class).hasSingleBean(AbacEvaluator.class);
            assertThat(context.getBean(AbacEvaluator.class)).isInstanceOf(SpelAbacEvaluator.class);
            assertThat(context.getBean(AbacTemplate.class).authorize(request).get().isRight()).isTrue();
        });
    }

    @Test
    void shouldBackOffWhenCustomEvaluatorIsContributed() {
        final AbacEvaluator custom = (expression, context) -> Either.right(false);
        runner.withBean(AbacEvaluator.class, () -> custom)
                .withBean(AbacPolicyLoader.class, () -> context -> List.of(
                        new AbacPolicy("1", "t001", "update", "true", true, Option.none())))
                .run(context -> {
                    assertThat(context).hasSingleBean(AbacEvaluator.class);
                    assertThat(context.getBean(AbacEvaluator.class)).isSameAs(custom);
                    assertThat(context.getBean(AbacTemplate.class).authorize(request).get().getLeft().code())
                            .isEqualTo("POLICY_DENIED");
                });
    }

    @Test
    void shouldFailWithoutPolicyLoader() {
        runner.run(context -> assertThat(context).hasFailed());
    }

    @Test
    void shouldPreserveRequiredPermissionConfiguration() {
        runner.withBean(AbacPolicyLoader.class, () -> context -> List.of())
                .withPropertyValues("security.abac.required-permissions[0]=update")
                .run(context -> assertThat(context.getBean(AbacTemplate.class).authorize(request).get().getLeft().code())
                        .isEqualTo("POLICY_MISSING"));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Application {
    }
}
