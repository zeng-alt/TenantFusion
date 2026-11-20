package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.security.api.WhiteListService;
import com.github.zeng.alt.security.api.WhiteListRule;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.core.WhiteListTemplate;
import com.github.zeng.alt.security.core.web.CompositeAuthorizationManager;
import com.github.zeng.alt.security.core.web.WhiteListAuthorizationManager;
import com.github.zeng.alt.security.rbac.serve.router.RouteTemplateManager;
import com.github.zeng.alt.tenant.local.TenantServiceAuthorizationManager;
import com.github.zeng.alt.security.abac.server.security.AbacServiceAuthorizationManager;
import com.github.zeng.alt.tenant.local.TenantServiceKeyFilter;
import com.github.zeng.alt.tenant.local.TenantStoreProperties;
import io.vavr.control.Either;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP 链路验证：基础授权允许不能绕过 ABAC 拒绝，也不能到达业务写入口。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@SpringJUnitConfig(AbacHttpAuthorizationTest.Config.class)
@WebAppConfiguration
class AbacHttpAuthorizationTest {
    @Autowired private WebApplicationContext context;
    @Autowired private AdminAbacService service;
    @Autowired private WriteEndpoint endpoint;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        reset(service);
        when(service.authorizeHttp(any(), any(), any())).thenReturn(Either.left("未通过用户授权"));
        endpoint.writes.set(0);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void shouldDenyBeforeReachingBusinessWrite() throws Exception {
        when(service.authorizeHttp(any(), eq("PATCH"), eq("/v1/user/{id}")))
                .thenReturn(Either.left("不满足资源访问条件"));
        mvc.perform(patch("/v1/user/42")).andExpect(status().isForbidden());
        assertThat(endpoint.writes).hasValue(0);
    }

    @Test
    void shouldGrantWhenBaseAndAbacBothAllow() throws Exception {
        when(service.authorizeHttp(any(), eq("PATCH"), eq("/v1/user/{id}")))
                .thenReturn(Either.right(true));
        mvc.perform(patch("/v1/user/42")).andExpect(status().isOk());
        assertThat(endpoint.writes).hasValue(1);
    }

    @Test
    void shouldFailClosedWhenContextResolutionThrows() throws Exception {
        when(service.authorizeHttp(any(), any(), any())).thenThrow(new IllegalStateException("unavailable"));
        mvc.perform(patch("/v1/user/42")).andExpect(status().isForbidden());
        assertThat(endpoint.writes).hasValue(0);
    }

    @Test
    void shouldAllowTenantMetadataWithValidServiceKey() throws Exception {
        mvc.perform(get("/v1/tenants/master").header(TenantServiceKeyFilter.SERVICE_KEY_HEADER, "test-service-key"))
                .andExpect(status().isOk());
        verifyNoInteractions(service);
    }

    @Test
    void shouldRejectTenantMetadataWithInvalidServiceKey() throws Exception {
        mvc.perform(get("/v1/tenants/master").header(TenantServiceKeyFilter.SERVICE_KEY_HEADER, "wrong-key"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectServiceKeyOnBusinessWrite() throws Exception {
        mvc.perform(patch("/v1/user/42").header(TenantServiceKeyFilter.SERVICE_KEY_HEADER, "test-service-key"))
                .andExpect(status().isForbidden());
        assertThat(endpoint.writes).hasValue(0);
    }

    @Test
    void shouldSkipHttpAbacForPublicWhitelist() throws Exception {
        mvc.perform(get("/public")).andExpect(status().isOk());
        verifyNoInteractions(service);
    }

    @Test
    void shouldSkipHttpAbacForLoginWhitelistAfterBaselineValidation() throws Exception {
        when(service.getAuthenticatedUser(any())).thenReturn(Either.right(
                SecurityUser.withUsername("user").id("7").tenant("t001").password("unused").roles().build()));
        mvc.perform(patch("/profile/7").with(user("user"))).andExpect(status().isOk());
        verify(service).getAuthenticatedUser(any());
        verify(service, never()).authorizeHttp(any(), any(), any());
        assertThat(endpoint.writes).hasValue(1);
    }

    @Test
    void shouldRejectLoginWhitelistWhenTenantBaselineFails() throws Exception {
        when(service.getAuthenticatedUser(any())).thenReturn(Either.left("认证租户与当前数据租户不一致"));
        mvc.perform(patch("/profile/7").with(user("user"))).andExpect(status().isForbidden());
        verify(service, never()).authorizeHttp(any(), any(), any());
        assertThat(endpoint.writes).hasValue(0);
    }

    @Test
    void shouldRejectAnonymousBeforeLoginWhitelistBaseline() throws Exception {
        mvc.perform(patch("/profile/7")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
        assertThat(endpoint.writes).hasValue(0);
    }

    @Test
    void shouldKeepHttpAbacForOtherMethodsOnLoginWhitelistPath() throws Exception {
        mvc.perform(get("/profile/7").with(user("user"))).andExpect(status().isForbidden());
        verify(service).authorizeHttp(any(), eq("GET"), any());
        assertThat(endpoint.writes).hasValue(0);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @EnableWebSecurity
    static class Config {
        @Bean
        AdminAbacService service() {
            return mock(AdminAbacService.class);
        }

        @Bean
        RouteTemplateManager routes() {
            final var routes = mock(RouteTemplateManager.class);
            when(routes.match("PATCH", "/v1/user/42")).thenReturn("/v1/user/{id}");
            return routes;
        }

        @Bean
        WhiteListService whiteList() {
            return new WhiteListService() {
                @Override
                public Set<String> getWhiteList() {
                    return Set.of("/public");
                }

                @Override
                public Set<WhiteListRule> getAuthenticated() {
                    return Set.of(new WhiteListRule("PATCH", "/profile/{id}"));
                }
            };
        }

        @Bean
        WhiteListTemplate whiteListTemplate(WhiteListService service) {
            return new WhiteListTemplate(service);
        }

        @Bean
        TenantStoreProperties tenantStoreProperties() {
            final var properties = new TenantStoreProperties();
            properties.setServiceKey("test-service-key");
            return properties;
        }

        @Bean
        TenantServiceAuthorizationManager tenantServiceAuthorizationManager(TenantStoreProperties properties) {
            return new TenantServiceAuthorizationManager(properties);
        }

        @Bean
        AdminAbacAuthorizationConstraint constraint(
                AdminAbacService service,
                RouteTemplateManager routes,
                WhiteListTemplate whiteList,
                ObjectProvider<TenantServiceAuthorizationManager> tenantServices,
                ObjectProvider<AbacServiceAuthorizationManager> abacServices
        ) {
            return new AdminAbacAuthorizationConstraint(service, routes, whiteList, tenantServices, abacServices);
        }

        @Bean
        SecurityFilterChain security(
                HttpSecurity http,
                AdminAbacAuthorizationConstraint constraint,
                TenantStoreProperties properties,
                WhiteListTemplate whiteList
        ) throws Exception {
            final var composite = new CompositeAuthorizationManager(
                    List.of(new WhiteListAuthorizationManager(whiteList,
                            (authentication, request) -> new AuthorizationDecision(true))), List.of(constraint)
            );
            return http.csrf(AbstractHttpConfigurer::disable)
                    .addFilterBefore(new TenantServiceKeyFilter(properties), UsernamePasswordAuthenticationFilter.class)
                    .authorizeHttpRequests(requests -> requests.anyRequest().access(composite)).build();
        }

        @Bean
        WriteEndpoint endpoint() {
            return new WriteEndpoint();
        }
    }

    @RestController
    static class WriteEndpoint {
        final AtomicInteger writes = new AtomicInteger();

        @GetMapping("/v1/tenants/master")
        String tenant() {
            return "master";
        }

        @GetMapping("/public")
        String publicEndpoint() {
            return "public";
        }

        @PatchMapping("/profile/{id}")
        String profile() {
            writes.incrementAndGet();
            return "updated";
        }

        @PatchMapping("/v1/user/{id}")
        String update() {
            writes.incrementAndGet();
            return "updated";
        }
    }
}
