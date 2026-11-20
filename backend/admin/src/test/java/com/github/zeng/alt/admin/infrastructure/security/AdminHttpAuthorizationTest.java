package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.admin.infrastructure.entity.User;
import com.github.zeng.alt.admin.infrastructure.repository.UserRepository;
import com.github.zeng.alt.admin.interfaces.rest.LoginLogController;
import com.github.zeng.alt.admin.interfaces.rest.OperLogController;
import com.github.zeng.alt.log.jpa.repository.LogRepository;
import com.github.zeng.alt.log.jpa.repository.LoginLogRepository;
import com.github.zeng.alt.security.abac.AbacAuthorizationBuilder;
import com.github.zeng.alt.security.abac.AbacFailure;
import com.github.zeng.alt.security.abac.AbacTemplate;
import com.github.zeng.alt.security.abac.server.config.AbacServerAutoConfiguration;
import com.github.zeng.alt.security.abac.server.spi.AbacPolicyRepository;
import com.github.zeng.alt.security.api.AuthorizationConstraint;
import com.github.zeng.alt.security.api.AuthorizationManagerProvider;
import com.github.zeng.alt.security.api.HttpResource;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.core.SecurityAutoConfiguration;
import com.github.zeng.alt.security.core.WhiteListTemplate;
import com.github.zeng.alt.security.core.properties.SecurityProperties;
import com.github.zeng.alt.security.core.web.config.WebSecurityAutoConfiguration;
import com.github.zeng.alt.security.jwt.JwtAuthAutoConfiguration;
import com.github.zeng.alt.security.jwt.JwtStorage;
import com.github.zeng.alt.security.jwt.JwtTokenProvider;
import com.github.zeng.alt.security.rbac.serve.manager.AdminAuthorizationManager;
import com.github.zeng.alt.security.rbac.serve.repository.RbacResourceService;
import com.github.zeng.alt.security.rbac.serve.router.RouteTemplateManager;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import com.github.zeng.alt.tenant.local.TenantServiceAuthorizationManager;
import com.github.zeng.alt.security.abac.server.security.AbacServiceAuthorizationManager;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 实际 JWT、RBAC 管理员入口和后置约束对日志接口的组合回归。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class AdminHttpAuthorizationTest {
    @AfterEach
    void clearContexts() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/v1/log/login/list", "/v1/log/login/42", "/v1/log/oper/list", "/v1/log/oper/42"})
    void adminIdCanReadLogsWithoutRegisteredHttpPermissions(String path) {
        final SecurityUser admin = user("1");
        runner(admin).run(context -> {
            mvc(context).perform(get(path).header("Authorization", bearer(context, admin))).andExpect(status().isOk());
            assertThat(context.getBean(Endpoints.class).permissionCalls).hasValue(0);
            verifyNoInteractions(context.getBean(RbacResourceService.class), context.getBean(AbacTemplate.class),
                    context.getBean(UserRepository.class));
        });
    }

    @Test
    void configuredCurrentAdminRoleAlsoBypassesHttpPermissions() {
        final SecurityUser admin = user("7");
        final var role = new SimpleGrantedAuthority("ADMIN");
        admin.getRoles().add(role);
        admin.setCurrentRole(role);
        runner(admin).run(context -> {
            mvc(context).perform(get("/v1/log/oper/list").header("Authorization", bearer(context, admin)))
                    .andExpect(status().isOk());
            assertThat(context.getBean(Endpoints.class).permissionCalls).hasValue(0);
            verifyNoInteractions(context.getBean(RbacResourceService.class), context.getBean(AbacTemplate.class));
        });
    }

    @Test
    void ordinaryUserStillNeedsHttpPermissions() {
        final SecurityUser user = user("7");
        runner(user).run(context -> {
            mvc(context).perform(get("/v1/log/oper/list").header("Authorization", bearer(context, user)))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(context.getBean(LogRepository.class));
        });
    }

    @Test
    void ordinaryUserWithBasePermissionDoesNotEvaluateHttpAbac() {
        final SecurityUser user = user("7");
        runner(user).run(context -> {
            context.getBean(Endpoints.class).ordinaryAllowed.set(true);
            final RbacResourceService rbac = context.getBean(RbacResourceService.class);
            when(rbac.findPermission(eq("t001"), any(HttpResource.class))).thenReturn("log:read");
            when(rbac.findRolePermission(any(), eq("7"), eq("t001"))).thenReturn(Set.of("log:read"));
            mvc(context).perform(get("/v1/log/oper/list").header("Authorization", bearer(context, user)))
                    .andExpect(status().isOk());
            verifyNoInteractions(context.getBean(AbacTemplate.class), context.getBean(UserRepository.class));
        });
    }

    @Test
    void disabledAdminExemptionUsesOrdinaryAuthorization() {
        final SecurityUser admin = user("1");
        runner(admin).withPropertyValues("security.context.admin.enabled=false").run(context -> {
            mvc(context).perform(get("/v1/log/oper/list").header("Authorization", bearer(context, admin)))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(context.getBean(LogRepository.class));
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"disabled", "locked", "expired", "credentialsExpired"})
    void invalidAdminAccountCannotUseItsCachedJwt(String state) {
        final SecurityUser admin = user("1");
        switch (state) {
            case "disabled" -> admin.setEnabled(false);
            case "locked" -> admin.setAccountNonLocked(false);
            case "expired" -> admin.setAccountNonExpired(false);
            case "credentialsExpired" -> admin.setCredentialsNonExpired(false);
            default -> throw new IllegalArgumentException(state);
        }
        runner(admin).run(context -> {
            mvc(context).perform(get("/v1/log/oper/list").header("Authorization", bearer(context, admin)))
                    .andExpect(status().isUnauthorized());
            verifyNoInteractions(context.getBean(LogRepository.class));
        });
    }

    @Test
    void deletedAdminCannotUseItsCachedJwt() {
        final SecurityUser admin = user("1");
        runner(admin).withAllowBeanDefinitionOverriding(true).withBean(UserDetailsService.class,
                () -> username -> { throw new UsernameNotFoundException(username); }).run(context -> {
            mvc(context).perform(get("/v1/log/oper/list").header("Authorization", bearer(context, admin)))
                    .andExpect(status().isUnauthorized());
            verifyNoInteractions(context.getBean(LogRepository.class));
        });
    }

    @Test
    void adminCannotBypassTenantConsistency() {
        final SecurityUser admin = user("1");
        runner(admin).withAllowBeanDefinitionOverriding(true).withBean(UserDetailsService.class, () -> username -> {
            TenantContextHolder.setTenantId("other");
            return admin;
        }).run(context -> {
            mvc(context).perform(get("/v1/log/oper/list").header("Authorization", bearer(context, admin)))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(context.getBean(LogRepository.class), context.getBean(RbacResourceService.class));
        });
    }

    @Test
    void adminStillRequiresOtherGlobalConstraints() {
        final SecurityUser admin = user("1");
        runner(admin).withUserConfiguration(DenyingConstraints.class).run(context -> {
            mvc(context).perform(get("/v1/log/oper/list").header("Authorization", bearer(context, admin)))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(context.getBean(LogRepository.class));
        });
    }

    @Test
    void anonymousRequestRemainsUnauthorized() {
        runner(user("1")).run(context -> {
            mvc(context).perform(get("/v1/log/oper/list")).andExpect(status().isUnauthorized());
            verifyNoInteractions(context.getBean(LogRepository.class));
        });
    }

    @Test
    void policyServiceIdentityPassesAdminConstraintWithoutBecomingAnAdminUser() {
        runner(user("1")).withConfiguration(AutoConfigurations.of(AbacServerAutoConfiguration.class))
                .withBean(AbacPolicyRepository.class, () -> query -> Either.right(List.of()))
                .withPropertyValues("security.abac.server.enabled=true",
                        "security.abac.server.clients.workflow.key=test-key",
                        "security.abac.server.clients.workflow.tenants[0]=t001")
                .run(context -> {
                    mvc(context).perform(get("/internal/abac/policies").param("tenant", "t001").param("permission", "read")
                                    .header("X-Abac-Client", "workflow").header("X-Abac-Service-Key", "test-key")
                                    .header("X-Tenant-Id", "t001"))
                            .andExpect(status().isOk());
                    verifyNoInteractions(context.getBean(UserRepository.class), context.getBean(AbacTemplate.class));
                    mvc(context).perform(get("/v1/log/oper/list")
                                    .header("X-Abac-Client", "workflow").header("X-Abac-Service-Key", "test-key")
                                    .header("X-Tenant-Id", "t001"))
                            .andExpect(status().isUnauthorized());
                });
    }

    private WebApplicationContextRunner runner(SecurityUser user) {
        final byte[] secret = new byte[192];
        new SecureRandom().nextBytes(secret);
        return new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(
                        SecurityAutoConfiguration.class,
                        WebSecurityAutoConfiguration.class,
                        JwtAuthAutoConfiguration.class
                ))
                .withUserConfiguration(Endpoints.class)
                .withBean(UserDetailsService.class, () -> username -> user)
                .withPropertyValues("security.username-login.enabled=false", "security.jwt-auth.authentication=true",
                        "security.jwt-auth.validation=true", "security.context.admin.code=admin",
                        "security.jwt-auth.base64-secret=" + Base64.getEncoder().encodeToString(secret));
    }

    private MockMvc mvc(WebApplicationContext context) {
        return MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private String bearer(WebApplicationContext context, SecurityUser user) {
        return "Bearer " + context.getBean(JwtTokenProvider.class).createToken(user);
    }

    private SecurityUser user(String id) {
        return SecurityUser.withUsername("admin").id(id).tenant("t001").password("unused").roles().build();
    }

    @Configuration(proxyBeanMethods = false)
    static class DenyingConstraints {
        @Bean
        AuthorizationConstraint<RequestAuthorizationContext> vetoConstraint() {
            return (authentication, request) -> new AuthorizationDecision(false);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import({LoginLogController.class, OperLogController.class})
    static class Endpoints {
        private final AtomicBoolean ordinaryAllowed = new AtomicBoolean();
        private final AtomicInteger permissionCalls = new AtomicInteger();

        @Bean
        ObjectMapper objectMapper() {
            return JsonMapper.builder().build();
        }

        @Bean
        JwtStorage jwtStorage() {
            final var storage = mock(JwtStorage.class);
            when(storage.hasToken(anyString())).thenReturn(true);
            return storage;
        }

        @Bean
        LoginLogRepository loginLogs() {
            final var logs = mock(LoginLogRepository.class);
            when(logs.findAll(any(Pageable.class))).thenReturn(Page.empty());
            when(logs.findById(42L)).thenReturn(Option.none());
            return logs;
        }

        @Bean
        LogRepository logs() {
            final var logs = mock(LogRepository.class);
            when(logs.findAll(any(Pageable.class))).thenReturn(Page.empty());
            when(logs.findById(42L)).thenReturn(Option.none());
            return logs;
        }

        @Bean
        UserRepository users() {
            return mock(UserRepository.class);
        }

        @Bean
        RbacResourceService rbac() {
            return mock(RbacResourceService.class);
        }

        @Bean
        AbacTemplate abac() {
            final var abac = mock(AbacTemplate.class);
            when(abac.authorize(any())).thenReturn(new AbacAuthorizationBuilder(
                    required -> Either.left(new AbacFailure("POLICY_DENIED", "不满足资源访问条件"))));
            return abac;
        }

        @Bean
        @Order(5)
        AuthorizationManagerProvider<RequestAuthorizationContext> administrators(SecurityProperties properties) {
            return () -> new AdminAuthorizationManager(properties);
        }

        @Bean
        @Order(10)
        AuthorizationManagerProvider<RequestAuthorizationContext> ordinaryPermissions() {
            return () -> (authentication, request) -> {
                permissionCalls.incrementAndGet();
                return new AuthorizationDecision(ordinaryAllowed.get());
            };
        }

        @Bean
        AdminAbacService abacService(
                AbacTemplate abac,
                RbacResourceService rbac,
                UserRepository users,
                SecurityProperties properties
        ) {
            return new AdminAbacService(rbac, users, properties, Mappers.getMapper(AbacUserMapper.class));
        }

        @Bean
        AdminAbacAuthorizationConstraint constraint(
                AdminAbacService service,
                WhiteListTemplate whiteList,
                ObjectProvider<TenantServiceAuthorizationManager> tenantServices,
                ObjectProvider<AbacServiceAuthorizationManager> abacServices
        ) {
            final var routes = mock(RouteTemplateManager.class);
            when(routes.match(anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
            return new AdminAbacAuthorizationConstraint(service, routes, whiteList, tenantServices, abacServices);
        }
    }
}
