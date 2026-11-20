package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.admin.interfaces.rest.MenuResourceController;
import com.github.zeng.alt.admin.interfaces.rest.TenantSwitchController;
import com.github.zeng.alt.admin.infrastructure.repository.UserRepository;
import com.github.zeng.alt.admin.query.api.MenuResourceService;
import com.github.zeng.alt.security.abac.AbacTemplate;
import com.github.zeng.alt.security.api.AuthorizationManagerProvider;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.core.SecurityAutoConfiguration;
import com.github.zeng.alt.security.core.WhiteListTemplate;
import com.github.zeng.alt.security.core.properties.SecurityProperties;
import com.github.zeng.alt.security.core.web.config.WebSecurityAutoConfiguration;
import com.github.zeng.alt.security.jwt.JwtAuthAutoConfiguration;
import com.github.zeng.alt.security.jwt.JwtStorage;
import com.github.zeng.alt.security.jwt.JwtTokenProvider;
import com.github.zeng.alt.security.rbac.serve.repository.RbacResourceService;
import com.github.zeng.alt.security.rbac.serve.router.RouteTemplateManager;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import com.github.zeng.alt.tenant.api.TenantMetadata;
import com.github.zeng.alt.tenant.api.TenantMetadataProvider;
import com.github.zeng.alt.tenant.local.TenantServiceAuthorizationManager;
import com.github.zeng.alt.security.abac.server.security.AbacServiceAuthorizationManager;
import io.vavr.control.Try;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 菜单树的实际 JWT、登录白名单、HTTP ABAC 与 MVC 组合链路回归。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class MenuResourceSecurityTest {
    @AfterEach
    void clearContexts() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    @Test
    void loggedInUserWithoutRolesCanReadAnEmptyMenuTree() {
        runner().run(context -> {
            assertThat(context).hasNotFailed();
            mvc(context).perform(get("/v1/menu/resource/tree").header("Authorization", bearer(context)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
            verify(context.getBean(MenuResourceService.class)).tree("7", null);
            verifyNoInteractions(context.getBean(RbacResourceService.class), context.getBean(AbacTemplate.class));
        });
    }

    @Test
    void authenticatedUserWithoutPermissionGetsForbiddenOnProtectedMenuEndpoint() {
        runner().run(context -> {
            mvc(context).perform(get("/v1/menu/resource/tree/all").header("Authorization", bearer(context)))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
            verifyNoInteractions(context.getBean(MenuResourceService.class));
        });
    }

    @Test
    void anonymousMenuTreeRequestGetsUnauthorized() {
        runner().run(context -> {
            mvc(context).perform(get("/v1/menu/resource/tree"))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
            verifyNoInteractions(context.getBean(MenuResourceService.class));
        });
    }

    @Test
    void invalidJwtMenuTreeRequestGetsUnauthorized() {
        runner().run(context -> {
            mvc(context).perform(get("/v1/menu/resource/tree").header("Authorization", "Bearer invalid"))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
            verifyNoInteractions(context.getBean(MenuResourceService.class));
        });
    }

    @Test
    void configuredIdentityCanValidateAndListTargetTenants() {
        runner().withPropertyValues("security.context.admin.id=7", "security.context.admin.tenant=t001")
                .run(context -> {
                    final var tenant = new TenantMetadata("t002", "租户二", null, null, null, null, null, true);
                    when(context.getBean(TenantMetadataProvider.class).findCurrentById("t002"))
                            .thenReturn(Optional.of(tenant));
                    when(context.getBean(TenantMetadataProvider.class).findAll()).thenReturn(List.of(tenant));
                    mvc(context).perform(get("/v1/auth/tenant").header("Authorization", bearer(context))
                                    .header("X-Active-Tenant", "t002"))
                            .andExpect(status().isOk()).andExpect(jsonPath("$.data").value("t002"));
                    mvc(context).perform(get("/v1/auth/tenants").header("Authorization", bearer(context)))
                            .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value("t002"));
                });
    }

    @Test
    void tenantAdminCannotSwitchOrListTenants() {
        runner().withPropertyValues("security.context.admin.id=7", "security.context.admin.tenant=master")
                .run(context -> {
                    mvc(context).perform(get("/v1/auth/tenant").header("Authorization", bearer(context))
                                    .header("X-Active-Tenant", "t002"))
                            .andExpect(status().isForbidden());
                    mvc(context).perform(get("/v1/auth/tenants").header("Authorization", bearer(context)))
                            .andExpect(status().isForbidden());
                });
    }

    private WebApplicationContextRunner runner() {
        final byte[] secret = new byte[192];
        new SecureRandom().nextBytes(secret);
        return new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(
                        SecurityAutoConfiguration.class,
                        WebSecurityAutoConfiguration.class,
                        JwtAuthAutoConfiguration.class
                ))
                .withUserConfiguration(Endpoints.class)
                .withInitializer(context -> Try.run(() -> new YamlPropertySourceLoader()
                        .load("admin", new ClassPathResource("application.yml"))
                        .forEach(context.getEnvironment().getPropertySources()::addLast)).get())
                .withPropertyValues("security.username-login.enabled=false", "security.jwt-auth.authentication=true",
                        "security.jwt-auth.validation=true",
                        "security.jwt-auth.base64-secret=" + Base64.getEncoder().encodeToString(secret));
    }

    private MockMvc mvc(WebApplicationContext context) {
        return MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private String bearer(WebApplicationContext context) {
        return "Bearer " + context.getBean(JwtTokenProvider.class).createToken(user());
    }

    private static SecurityUser user() {
        return SecurityUser.withUsername("menu-user").id("7").tenant("t001").password("unused").roles().build();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import({MenuResourceController.class, TenantSwitchController.class})
    static class Endpoints {
        @Bean
        ObjectMapper objectMapper() {
            return JsonMapper.builder().build();
        }

        @Bean
        UserDetailsService users() {
            return username -> user();
        }

        @Bean
        JwtStorage jwtStorage() {
            final var storage = mock(JwtStorage.class);
            when(storage.hasToken(anyString())).thenReturn(true);
            return storage;
        }

        @Bean
        TenantMetadataProvider tenantMetadataProvider() {
            return mock(TenantMetadataProvider.class);
        }

        @Bean
        MenuResourceService menus() {
            final var menus = mock(MenuResourceService.class);
            when(menus.tree("7", null)).thenReturn(List.of());
            return menus;
        }

        @Bean
        RbacResourceService permissions() {
            return mock(RbacResourceService.class);
        }

        @Bean
        AbacTemplate abac() {
            return mock(AbacTemplate.class);
        }

        @Bean
        AuthorizationManagerProvider<RequestAuthorizationContext> denyingProvider() {
            return () -> (authentication, request) -> new AuthorizationDecision(false);
        }

        @Bean
        AdminAbacService abacService(
                AbacTemplate abac,
                RbacResourceService permissions,
                SecurityProperties properties
        ) {
            return new AdminAbacService(permissions,
                mock(UserRepository.class),
                properties,
                mock(AbacUserMapper.class));
        }

        @Bean
        AdminAbacAuthorizationConstraint constraint(
                AdminAbacService service,
                WhiteListTemplate whiteList,
                ObjectProvider<TenantServiceAuthorizationManager> tenantServices,
                ObjectProvider<AbacServiceAuthorizationManager> abacServices
        ) {
            return new AdminAbacAuthorizationConstraint(
                    service,
                    mock(RouteTemplateManager.class),
                    whiteList,
                    tenantServices,
                    abacServices
            );
        }
    }
}
