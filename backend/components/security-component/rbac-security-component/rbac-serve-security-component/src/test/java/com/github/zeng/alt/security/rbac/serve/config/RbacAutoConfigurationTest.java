package com.github.zeng.alt.security.rbac.serve.config;

import com.github.zeng.alt.security.api.AuthorizationManagerProvider;
import com.github.zeng.alt.security.api.AuthHelper;
import com.github.zeng.alt.security.api.AuthorityResolver;
import com.github.zeng.alt.security.api.ReactiveAuthorizationManagerProvider;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.api.UserContextHolder;
import com.github.zeng.alt.security.core.SecurityAutoConfiguration;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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

    @Test
    void shouldResolveHelperPermissionsFromActiveRoleUserAndTenant() {
        final RbacResourceService resources = mock(RbacResourceService.class);
        when(resources.findRolePermission(List.of("USER"), "7", "master"))
                .thenReturn(Set.of("dict:type:update"));
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(
                        SecurityAutoConfiguration.class, RbacAutoConfiguration.class))
                .withBean(RbacResourceService.class, () -> resources)
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(AuthorityResolver.class);
                    final SimpleGrantedAuthority role = new SimpleGrantedAuthority("USER");
                    final SecurityUser user = SecurityUser.withUsername("review").id("7").tenant("master")
                            .roles(Set.of(role, new SimpleGrantedAuthority("ADMIN"))).currentRole(role).build();
                    UserContextHolder.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                            user, null, user.getAuthorities()));
                    try {
                        final AuthHelper helper = context.getBean(AuthHelper.class);
                        assertThat(helper.hasAnyAuthority("missing", "dict:type:update")).isTrue();
                        assertThat(helper.hasRole("ADMIN")).isFalse();
                        verify(resources).findRolePermission(List.of("USER"), "7", "master");
                    } finally {
                        UserContextHolder.clear();
                    }
                });
    }

    @Test
    void shouldNotResolveRbacPermissionsWithoutAuthentication() {
        final RbacResourceService resources = mock(RbacResourceService.class);
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(
                        SecurityAutoConfiguration.class, RbacAutoConfiguration.class))
                .withBean(RbacResourceService.class, () -> resources)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(AuthHelper.class).hasAuthority("dict:type:update")).isFalse();
                    verifyNoInteractions(resources);
                });
    }

    private String[] properties(String setting) {
        return "missing".equals(setting) ? new String[0]
                : new String[]{"security.context.enabled-access=" + setting};
    }
}
