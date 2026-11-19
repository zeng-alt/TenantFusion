package com.github.zeng.alt.security.cookie;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.core.SecurityAutoConfiguration;
import com.github.zeng.alt.security.core.web.config.WebSecurityAutoConfiguration;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import com.github.zeng.alt.tenant.api.TenantRouting;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * 验证 Cookie 认证的完整安全链默认拒绝无 CSRF token 的写请求。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class CookieSecurityTest {
    private final AtomicReference<SecurityUser> currentUser = new AtomicReference<>(user());
    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration.class,
                    WebSecurityAutoConfiguration.class, CookieAuthAutoConfiguration.class))
            .withPropertyValues("security.username-login.enabled=false", "security.cookie-auth.validation=true")
            .withBean(ObjectMapper.class, () -> JsonMapper.builder().build())
            .withBean(AuthenticationManager.class, () -> authentication -> authentication)
            .withBean(UserDetailsService.class, () -> username -> {
                assertThat(TenantContextHolder.getTenantId()).isEqualTo("t001");
                if (currentUser.get() == null) {
                    throw new UsernameNotFoundException(username);
                }
                return currentUser.get();
            })
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
        TenantContextHolder.clear();
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

    @Test
    void shouldBindAuthenticatedTenantAndDiscardUntrustedRouting() {
        runner.run(context -> {
            TenantContextHolder.setTenantId("attacker-tenant");
            TenantContextHolder.setRouting(TenantRouting.none("attacker-tenant"));
            TenantContextHolder.setDatabase("attacker-db");
            TenantContextHolder.setSchema("attacker-schema");
            final var reached = new AtomicBoolean();
            new FilterChainProxy(context.getBean(SecurityFilterChain.class)).doFilter(
                    request("GET"), new MockHttpServletResponse(), (req, res) -> {
                        reached.set(true);
                        assertThat(TenantContextHolder.getTenantId()).isEqualTo("t001");
                        assertThat(TenantContextHolder.getRouting()).isNull();
                        assertThat(TenantContextHolder.getDatabase()).isNull();
                        assertThat(TenantContextHolder.getSchema()).isNull();
                    });
            assertThat(reached).isTrue();
            assertThat(TenantContextHolder.getTenantId()).isNull();
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"disabled", "locked", "expired", "credentialsExpired", "deleted", "differentId", "differentTenant"})
    void shouldRejectStaleSessionWhenAccountChanges(String change) {
        final var user = user();
        switch (change) {
            case "disabled" -> user.setEnabled(false);
            case "locked" -> user.setAccountNonLocked(false);
            case "expired" -> user.setAccountNonExpired(false);
            case "credentialsExpired" -> user.setCredentialsNonExpired(false);
            case "differentId" -> user.setId("8");
            default -> { }
        }
        currentUser.set(switch (change) {
            case "deleted" -> null;
            case "differentTenant" -> SecurityUser.withUsername("review").id("7")
                    .tenant("other").password("unused").build();
            default -> user;
        });
        runner.run(context -> {
            final var response = new MockHttpServletResponse();
            final var reached = new AtomicBoolean();
            new FilterChainProxy(context.getBean(SecurityFilterChain.class)).doFilter(
                    request("GET"), response, (req, res) -> reached.set(true));
            assertThat(reached).isFalse();
            assertThat(response.getStatus()).isEqualTo(401);
            org.mockito.Mockito.verify(context.getBean(SessionManager.class)).removeSession("valid-session");
            assertThat(TenantContextHolder.getTenantId()).isNull();
        });
    }

    private static SecurityUser user() {
        return SecurityUser.withUsername("review").id("7").tenant("t001").password("unused").build();
    }

    @Test
    void shouldUseFreshRolesAndEraseLoadedCredentials() {
        final var fresh = user();
        fresh.getRoles().add(new SimpleGrantedAuthority("new-role"));
        currentUser.set(fresh);
        runner.run(context -> new FilterChainProxy(context.getBean(SecurityFilterChain.class)).doFilter(
                request("GET"), new MockHttpServletResponse(), (req, res) -> {
                    final var user = (SecurityUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                    assertThat(user.getRoles()).extracting(role -> role.getAuthority()).containsExactly("new-role");
                    assertThat(user.getPassword()).isNull();
                }));
    }

    @Test
    void shouldClearContextsWhenAccountLookupFails() {
        runner.withAllowBeanDefinitionOverriding(true).withBean(UserDetailsService.class,
                () -> username -> { throw new IllegalStateException("database unavailable"); }).run(context -> {
                    assertThatThrownBy(() -> new FilterChainProxy(context.getBean(SecurityFilterChain.class)).doFilter(
                            request("GET"), new MockHttpServletResponse(), (req, res) -> { }))
                            .isInstanceOf(IllegalStateException.class).hasMessage("database unavailable");
                    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
                    assertThat(TenantContextHolder.getTenantId()).isNull();
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
