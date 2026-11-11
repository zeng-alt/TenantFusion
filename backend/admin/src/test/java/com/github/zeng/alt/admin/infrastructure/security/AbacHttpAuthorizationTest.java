package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.security.api.WhiteListService;
import com.github.zeng.alt.security.core.web.CompositeAuthorizationManager;
import com.github.zeng.alt.security.rbac.serve.router.RouteTemplateManager;
import io.vavr.control.Either;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
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
        endpoint.writes.set(0);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void shouldDenyBeforeReachingBusinessWrite() throws Exception {
        when(service.authorizeHttp(any(), eq("PATCH"), eq("/v1/user/42"), eq("/v1/user/{id}")))
                .thenReturn(Either.left("不满足资源访问条件"));
        mvc.perform(patch("/v1/user/42")).andExpect(status().isForbidden());
        assertThat(endpoint.writes).hasValue(0);
    }

    @Test
    void shouldGrantWhenBaseAndAbacBothAllow() throws Exception {
        when(service.authorizeHttp(any(), eq("PATCH"), eq("/v1/user/42"), eq("/v1/user/{id}")))
                .thenReturn(Either.right(true));
        mvc.perform(patch("/v1/user/42")).andExpect(status().isOk());
        assertThat(endpoint.writes).hasValue(1);
    }

    @Test
    void shouldFailClosedWhenContextResolutionThrows() throws Exception {
        when(service.authorizeHttp(any(), any(), any(), any())).thenThrow(new IllegalStateException("unavailable"));
        mvc.perform(patch("/v1/user/42")).andExpect(status().isForbidden());
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
            return Set::of;
        }

        @Bean
        AdminAbacAuthorizationConstraint constraint(AdminAbacService service, RouteTemplateManager routes, WhiteListService whiteList) {
            return new AdminAbacAuthorizationConstraint(service, routes, whiteList);
        }

        @Bean
        SecurityFilterChain security(HttpSecurity http, AdminAbacAuthorizationConstraint constraint) throws Exception {
            final var composite = new CompositeAuthorizationManager(
                    List.of((authentication, request) -> new AuthorizationDecision(true)), List.of(constraint)
            );
            return http.csrf(AbstractHttpConfigurer::disable)
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

        @PatchMapping("/v1/user/{id}")
        String update() {
            writes.incrementAndGet();
            return "updated";
        }
    }
}
