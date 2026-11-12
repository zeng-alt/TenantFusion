package com.github.zeng.alt.security.cookie;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.core.SecurityAutoConfiguration;
import com.github.zeng.alt.security.core.web.config.WebSecurityAutoConfiguration;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 验证 Cookie 认证的完整安全链默认拒绝无 CSRF token 的写请求。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class CookieSecurityTest {
    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration.class,
                    WebSecurityAutoConfiguration.class, CookieAuthAutoConfiguration.class))
            .withPropertyValues("security.username-login.enabled=false", "security.cookie-auth.validation=true")
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withBean(AuthenticationManager.class, () -> authentication -> authentication)
            .withBean(SessionManager.class, () -> {
                final var sessions = mock(SessionManager.class);
                when(sessions.getSession("valid-session")).thenReturn(SecurityUser.withUsername("review")
                        .id("7").tenant("t001").password("unused").build());
                return sessions;
            })
            .withBean("mvcHandlerMappingIntrospector", HandlerMappingIntrospector.class,
                    HandlerMappingIntrospector::new);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void shouldRejectCredentialedWritesWithoutCsrfToken(String method) {
        runner.run(context -> {
            final var response = new MockHttpServletResponse();
            final var reached = new AtomicBoolean();
            new FilterChainProxy(context.getBean(SecurityFilterChain.class)).doFilter(
                    request(method), response, (req, res) -> reached.set(true));
            assertThat(response.getStatus()).isEqualTo(403);
            assertThat(reached).isFalse();
        });
    }

    @Test
    void shouldIssueCsrfCookieAndAcceptOnlyMatchingHeader() {
        runner.run(context -> {
            final var proxy = new FilterChainProxy(context.getBean(SecurityFilterChain.class));
            final var response = new MockHttpServletResponse();
            proxy.doFilter(request("GET"), response, (req, res) -> { });
            final Cookie csrf = response.getCookie("XSRF-TOKEN");
            assertThat(csrf).isNotNull();
            for (final String token : new String[] {"wrong-token", csrf.getValue()}) {
                final var write = request("POST");
                write.setCookies(new Cookie("SESSION", "valid-session"), csrf);
                write.addHeader("X-XSRF-TOKEN", token);
                final var written = new AtomicBoolean();
                final var writeResponse = new MockHttpServletResponse();
                proxy.doFilter(write, writeResponse, (req, res) -> written.set(true));
                assertThat(written.get()).isEqualTo(token.equals(csrf.getValue()));
                assertThat(writeResponse.getStatus()).isEqualTo(token.equals(csrf.getValue()) ? 200 : 403);
            }
        });
    }

    @Test
    void shouldAllowExplicitCsrfOptOut() {
        runner.withPropertyValues("security.cookie-auth.csrf-enabled=false").run(context -> {
            final var reached = new AtomicBoolean();
            new FilterChainProxy(context.getBean(SecurityFilterChain.class)).doFilter(
                    request("POST"), new MockHttpServletResponse(), (req, res) -> reached.set(true));
            assertThat(reached).isTrue();
        });
    }

    private MockHttpServletRequest request(String method) {
        final var request = new MockHttpServletRequest(method, "/secured");
        request.setServletPath("/secured");
        request.setCookies(new Cookie("SESSION", "valid-session"));
        request.addHeader("Origin", "https://attacker.example");
        return request;
    }
}
