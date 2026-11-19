package com.github.zeng.alt.security.jwt;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.core.SecurityAutoConfiguration;
import com.github.zeng.alt.security.core.web.config.WebSecurityAutoConfiguration;
import com.github.zeng.alt.security.cookie.CookieAuthAutoConfiguration;
import com.github.zeng.alt.security.cookie.CookieAuthenticationFilter;
import com.github.zeng.alt.security.cookie.SessionManager;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import jakarta.servlet.http.Cookie;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 验证 JWT 与 Cookie 可以共同装配，且各自凭证仍通过同一安全链认证。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class AuthenticationModulesAutoConfigurationTest {
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldAssembleBothAuthenticationModulesWithoutBeanCollisions(boolean authentication) {
        runner(authentication).run(context -> {
            assertThat(context).hasNotFailed();
            final var filters = context.getBean(SecurityFilterChain.class).getFilters();
            assertThat(filters).anyMatch(JwtAuthenticationFilter.class::isInstance)
                    .anyMatch(CookieAuthenticationFilter.class::isInstance);
            if (authentication) {
                assertThat(context.getBeansOfType(OpenApiCustomizer.class)).hasSize(2);
            }
            final var proxy = new FilterChainProxy(context.getBean(SecurityFilterChain.class));
            final var jwtRequest = request();
            jwtRequest.addHeader("Authorization", "Bearer " + context.getBean(JwtTokenProvider.class).createToken(user()));
            assertAuthenticated(proxy, jwtRequest);
            final var cookieRequest = request();
            cookieRequest.setCookies(new Cookie("SESSION", "review-session"));
            assertAuthenticated(proxy, cookieRequest);
        });
    }

    private WebApplicationContextRunner runner(boolean authentication) {
        final byte[] secret = new byte[192];
        new SecureRandom().nextBytes(secret);
        final var storage = mock(JwtStorage.class);
        when(storage.hasToken(anyString())).thenReturn(true);
        final var sessions = mock(SessionManager.class);
        when(sessions.getSession("review-session")).thenReturn(user());
        return new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(
                        SecurityAutoConfiguration.class, WebSecurityAutoConfiguration.class,
                        JwtAuthAutoConfiguration.class, CookieAuthAutoConfiguration.class))
                .withPropertyValues("security.username-login.enabled=false", "security.jwt-auth.validation=true",
                        "security.cookie-auth.validation=true", "security.jwt-auth.authentication=" + authentication,
                        "security.cookie-auth.authentication=" + authentication,
                        "security.jwt-auth.base64-secret=" + Base64.getEncoder().encodeToString(secret))
                .withBean(ObjectMapper.class, () -> JsonMapper.builder().build())
                .withBean(AuthenticationManager.class, () -> request -> request)
                .withBean(UserDetailsService.class, () -> username -> user())
                .withBean(JwtStorage.class, () -> storage)
                .withBean(SessionManager.class, () -> sessions)
                .withBean("mvcHandlerMappingIntrospector", HandlerMappingIntrospector.class,
                        HandlerMappingIntrospector::new);
    }

    private void assertAuthenticated(FilterChainProxy proxy, MockHttpServletRequest request) throws Exception {
        final var reached = new AtomicBoolean();
        proxy.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            reached.set(true);
            assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("review");
        });
        assertThat(reached).isTrue();
    }

    private MockHttpServletRequest request() {
        final var request = new MockHttpServletRequest("GET", "/secured");
        request.setServletPath("/secured");
        return request;
    }

    private SecurityUser user() {
        return SecurityUser.withUsername("review").id("7").tenant("t001").password("unused").roles().build();
    }
}
