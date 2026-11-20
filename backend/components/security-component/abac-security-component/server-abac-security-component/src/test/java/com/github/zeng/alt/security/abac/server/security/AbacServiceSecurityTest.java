package com.github.zeng.alt.security.abac.server.security;

import com.github.zeng.alt.security.abac.server.config.AbacServerAutoConfiguration;
import com.github.zeng.alt.security.abac.server.spi.AbacPolicyRepository;
import com.github.zeng.alt.security.api.AuthorizationConstraint;
import com.github.zeng.alt.security.api.AuthorizationManagerProvider;
import com.github.zeng.alt.security.core.SecurityAutoConfiguration;
import com.github.zeng.alt.security.core.web.config.WebSecurityAutoConfiguration;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import io.vavr.control.Either;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 服务身份通过实际 Spring Security 链，只授权允许租户的策略查询。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class AbacServiceSecurityTest {
    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration.class,
                    WebSecurityAutoConfiguration.class, AbacServerAutoConfiguration.class))
            .withUserConfiguration(Application.class)
            .withPropertyValues("security.username-login.enabled=false", "security.abac.server.enabled=true",
                    "security.abac.server.clients.workflow.key=test-key",
                    "security.abac.server.clients.workflow.tenants[0]=t001");

    @AfterEach
    void clearContexts() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void serviceCanQueryWithoutUserAuthorization() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            mvc(context).perform(get("/internal/abac/policies").param("tenant", "t001").param("permission", "update")
                            .header("X-Abac-Client", "workflow").header("X-Abac-Service-Key", "test-key")
                            .header("X-Tenant-Id", "t001"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.tenant").value("t001"));
        });
    }

    @ParameterizedTest
    @CsvSource({"unknown,test-key", "workflow,wrong-key", "workflow,''", "'',test-key"})
    void rejectsInvalidServiceIdentity(String client, String key) {
        runner.run(context -> mvc(context).perform(serviceRequest("t001", "t001")
                        .header("X-Abac-Client", client).header("X-Abac-Service-Key", key))
                .andExpect(status().isUnauthorized()));
    }

    @ParameterizedTest
    @CsvSource({"t002,t002", "t001,t002", "'',t001"})
    void rejectsUnpermittedOrMismatchedTenant(String headerTenant, String queryTenant) {
        runner.run(context -> mvc(context).perform(serviceRequest(headerTenant, queryTenant)
                        .header("X-Abac-Client", "workflow").header("X-Abac-Service-Key", "test-key"))
                .andExpect(status().isForbidden()));
    }

    @Test
    void requiresServiceIdentityAndTenantHeader() {
        runner.run(context -> {
            final MockMvc mvc = mvc(context);
            mvc.perform(serviceRequest("t001", "t001")).andExpect(status().isUnauthorized());
            mvc.perform(get("/internal/abac/policies").param("tenant", "t001").param("permission", "update")
                            .header("X-Abac-Service-Key", "test-key"))
                    .andExpect(status().isUnauthorized());
            mvc.perform(get("/internal/abac/policies").param("tenant", "t001").param("permission", "update")
                            .header("X-Abac-Client", "workflow").header("X-Abac-Service-Key", "test-key"))
                    .andExpect(status().isForbidden());
        });
    }

    @Test
    void cannotUseServiceCredentialsToReadOtherEndpointsOrWritePolicies() {
        runner.run(context -> {
            final MockMvc mvc = mvc(context);
            mvc.perform(serviceRequest("t001", "t001").with(request -> {
                request.setRequestURI("/v1/user/detail");
                request.setServletPath("/v1/user/detail");
                return request;
            }).header("X-Abac-Client", "workflow").header("X-Abac-Service-Key", "test-key"))
                    .andExpect(status().isUnauthorized());
            mvc.perform(serviceRequest("t001", "t001").with(request -> {
                request.setMethod("POST");
                return request;
            }).header("X-Abac-Client", "workflow").header("X-Abac-Service-Key", "test-key"))
                    .andExpect(status().isUnauthorized());
        });
    }

    @Test
    void preservesGlobalConstraintAndReturns403ForAuthenticatedService() {
        runner.withUserConfiguration(DenyingConstraint.class).run(context ->
                mvc(context).perform(serviceRequest("t001", "t001")
                                .header("X-Abac-Client", "workflow").header("X-Abac-Service-Key", "test-key"))
                        .andExpect(status().isForbidden()));
    }

    @Test
    void managerOnlyGrantsMatchingServicePrincipalAndCurrentTenant() {
        final AbacServiceAuthorizationManager manager = new AbacServiceAuthorizationManager();
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/abac/policies");
        request.setServletPath("/internal/abac/policies");
        request.setParameter("tenant", "t001");
        final RequestAuthorizationContext context = new RequestAuthorizationContext(request);
        final AbacServiceAuthentication service = new AbacServiceAuthentication("workflow", "t001");
        assertThat(manager.get()).isSameAs(manager);
        assertThat(manager.getOrder()).isZero();
        assertThat(manager.authorize(() -> service, context).isGranted()).isFalse();
        TenantContextHolder.setTenantId("t001");
        assertThat(manager.authorize(() -> service, context).isGranted()).isTrue();
        assertThat(manager.authorize(() -> null, context).isGranted()).isFalse();
        request.setParameter("tenant", "t002");
        assertThat(manager.authorize(() -> service, context).isGranted()).isFalse();
        request.setParameter("tenant", "t001");
        service.setAuthenticated(false);
        assertThat(manager.authorize(() -> service, context).isGranted()).isFalse();
    }

    private MockHttpServletRequestBuilder serviceRequest(String headerTenant, String queryTenant) {
        return get("/internal/abac/policies").param("tenant", queryTenant).param("permission", "update")
                .header("X-Tenant-Id", headerTenant);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    static class Application {
        @Bean
        ObjectMapper objectMapper() {
            return JsonMapper.builder().build();
        }

        @Bean
        AuthenticationManager authenticationManager() {
            return authentication -> { throw new BadCredentialsException("user login not used"); };
        }

        @Bean
        AuthorizationManagerProvider<RequestAuthorizationContext> denyUserPermissions() {
            return () -> (authentication, request) -> new AuthorizationDecision(false);
        }

        @Bean
        AbacPolicyRepository repository() {
            return query -> {
                assertThat(TenantContextHolder.getTenantId()).isEqualTo(query.tenant());
                return Either.right(List.of());
            };
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class DenyingConstraint {
        @Bean
        AuthorizationConstraint<RequestAuthorizationContext> otherConstraint() {
            return (authentication, request) -> new AuthorizationDecision(false);
        }
    }

    private MockMvc mvc(WebApplicationContext context) {
        return MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
}
