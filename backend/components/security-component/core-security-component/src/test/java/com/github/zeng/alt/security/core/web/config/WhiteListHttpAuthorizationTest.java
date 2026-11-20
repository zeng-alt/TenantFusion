package com.github.zeng.alt.security.core.web.config;

import com.github.zeng.alt.security.api.AuthorizationManagerProvider;
import com.github.zeng.alt.security.api.AuthorizationConstraint;
import com.github.zeng.alt.security.core.SecurityAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 通过实际 Servlet 安全链验证两类白名单的终结授权语义。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class WhiteListHttpAuthorizationTest {
    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration.class, WebSecurityAutoConfiguration.class))
            .withPropertyValues("security.username-login.enabled=false",
                    "security.whitelist.permit-all[0].method=GET",
                    "security.whitelist.permit-all[0].path=/public",
                    "security.whitelist.authenticated[0].method=PATCH",
                    "security.whitelist.authenticated[0].path=/profile/{id}")
            .withBean(AuthenticationManager.class, () -> authentication -> authentication)
            .withBean(ObjectMapper.class, () -> JsonMapper.builder().build())
            .withBean("mvcHandlerMappingIntrospector", HandlerMappingIntrospector.class, HandlerMappingIntrospector::new);

    @Test
    void publicRuleAllowsAnonymousWithoutCallingProviders() {
        final var calls = new AtomicInteger();
        runner.withBean(AuthorizationManagerProvider.class, () -> provider(calls, false)).run(context -> {
            final var response = new MockHttpServletResponse();
            final var reached = new AtomicBoolean();
            new FilterChainProxy(context.getBean(SecurityFilterChain.class)).doFilter(
                    request("GET", "/public", false), response, (req, res) -> reached.set(true));
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(reached).isTrue();
            assertThat(calls).hasValue(0);
        });
    }

    @Test
    void loginRuleRejectsAnonymousEvenWhenAnotherProviderWouldGrant() {
        final var calls = new AtomicInteger();
        runner.withBean(AuthorizationManagerProvider.class, () -> provider(calls, true)).run(context -> {
            final var response = new MockHttpServletResponse();
            final var reached = new AtomicBoolean();
            new FilterChainProxy(context.getBean(SecurityFilterChain.class)).doFilter(
                    request("PATCH", "/profile/7", false), response, (req, res) -> reached.set(true));
            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(reached).isFalse();
            assertThat(calls).hasValue(0);
        });
    }

    @Test
    void loginRuleAllowsAuthenticatedWithoutCallingProviders() {
        final var calls = new AtomicInteger();
        runner.withBean(AuthorizationManagerProvider.class, () -> provider(calls, false)).run(context -> {
            final var response = new MockHttpServletResponse();
            final var reached = new AtomicBoolean();
            new FilterChainProxy(context.getBean(SecurityFilterChain.class)).doFilter(
                    request("PATCH", "/profile/7", true), response, (req, res) -> reached.set(true));
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(reached).isTrue();
            assertThat(calls).hasValue(0);
        });
    }

    @Test
    void anotherMethodRemainsSubjectToProviders() {
        final var calls = new AtomicInteger();
        runner.withBean(AuthorizationManagerProvider.class, () -> provider(calls, false)).run(context -> {
            final var response = new MockHttpServletResponse();
            final var reached = new AtomicBoolean();
            new FilterChainProxy(context.getBean(SecurityFilterChain.class)).doFilter(
                    request("DELETE", "/profile/7", true), response, (req, res) -> reached.set(true));
            assertThat(response.getStatus()).isEqualTo(403);
            assertThat(reached).isFalse();
            assertThat(calls).hasValue(1);
        });
    }

    @Test
    void loginWhitelistStillRequiresOtherGlobalConstraints() {
        runner.withBean(AuthorizationConstraint.class, () ->
                (AuthorizationConstraint<RequestAuthorizationContext>) (authentication, context) ->
                        new AuthorizationDecision(false)).run(context -> {
            final var response = new MockHttpServletResponse();
            final var reached = new AtomicBoolean();
            new FilterChainProxy(context.getBean(SecurityFilterChain.class)).doFilter(
                    request("PATCH", "/profile/7", true), response, (req, res) -> reached.set(true));
            assertThat(response.getStatus()).isEqualTo(403);
            assertThat(reached).isFalse();
        });
    }

    private AuthorizationManagerProvider<RequestAuthorizationContext> provider(AtomicInteger calls, boolean grant) {
        return () -> (authentication, request) -> {
            calls.incrementAndGet();
            return new AuthorizationDecision(grant);
        };
    }

    private MockHttpServletRequest request(String method, String path, boolean authenticated) {
        final var request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        if (authenticated) {
            request.getSession().setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    new SecurityContextImpl(new TestingAuthenticationToken("user", "unused", "ROLE_USER")));
        }
        return request;
    }
}
