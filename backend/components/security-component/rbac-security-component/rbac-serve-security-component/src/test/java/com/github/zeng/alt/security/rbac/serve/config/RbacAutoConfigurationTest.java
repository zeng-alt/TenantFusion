package com.github.zeng.alt.security.rbac.serve.config;

import com.github.zeng.alt.security.api.AuthorizationManagerProvider;
import com.github.zeng.alt.security.api.ReactiveAuthorizationManagerProvider;
import com.github.zeng.alt.security.core.properties.SecurityProperties;
import com.github.zeng.alt.security.rbac.serve.repository.RbacResourceLoader;
import com.github.zeng.alt.security.rbac.serve.repository.RbacResourceService;
import com.github.zeng.alt.storage.StorageTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 验证 RBAC 在两种 Web 栈下默认启用，并尊重显式关闭配置。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class RbacAutoConfigurationTest {
    @ParameterizedTest
    @ValueSource(strings = {"missing", "true", "false"})
    void shouldEnableServletRbacUnlessExplicitlyDisabled(String setting) {
        new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(
                        RbacAutoConfiguration.class, RbacWebAutoConfiguration.class))
                .withPropertyValues(properties(setting))
                .withBean(SecurityProperties.class, SecurityProperties::new)
                .withBean(StorageTemplate.class, () -> mock(StorageTemplate.class))
                .withBean(RbacResourceLoader.class, () -> mock(RbacResourceLoader.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    final boolean enabled = !"false".equals(setting);
                    assertThat(context.getBeansOfType(RbacResourceService.class)).hasSize(enabled ? 1 : 0);
                    assertThat(context.getBeansOfType(AuthorizationManagerProvider.class)).hasSize(enabled ? 2 : 0);
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "true", "false"})
    void shouldEnableReactiveRbacUnlessExplicitlyDisabled(String setting) {
        new ReactiveWebApplicationContextRunner().withConfiguration(AutoConfigurations.of(
                        RbacAutoConfiguration.class, RbacReactiveAutoConfiguration.class))
                .withPropertyValues(properties(setting))
                .withBean(SecurityProperties.class, SecurityProperties::new)
                .withBean(StorageTemplate.class, () -> mock(StorageTemplate.class))
                .withBean(RbacResourceLoader.class, () -> mock(RbacResourceLoader.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    final boolean enabled = !"false".equals(setting);
                    assertThat(context.getBeansOfType(RbacResourceService.class)).hasSize(enabled ? 1 : 0);
                    assertThat(context.getBeansOfType(ReactiveAuthorizationManagerProvider.class))
                            .hasSize(enabled ? 2 : 0);
                });
    }

    @Test
    void shouldFailStartupWithoutRequiredLoaderByDefault() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(RbacAutoConfiguration.class))
                .withBean(StorageTemplate.class, () -> mock(StorageTemplate.class))
                .run(context -> assertThat(context).hasFailed());
    }

    private String[] properties(String setting) {
        return "missing".equals(setting) ? new String[0]
                : new String[]{"security.context.enabled-access=" + setting};
    }
}
