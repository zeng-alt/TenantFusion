package com.github.zeng.alt.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.core.SecurityAutoConfiguration;
import com.github.zeng.alt.security.core.web.config.WebSecurityAutoConfiguration;
import com.github.zeng.alt.storage.StorageTemplate;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 按 workflow 的只验签配置验证默认登录入口和合法 JWT 请求。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class JwtAuthAutoConfigurationTest {
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldNotCreateAuthenticatedSessionWithDefaultCredentials() {
        runner().run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(SecurityFilterChain.class).getFilters())
                    .noneMatch(UsernamePasswordAuthenticationFilter.class::isInstance);
            final var login = request("POST", "/login");
            login.addParameter("username", "root");
            login.addParameter("password", "123456");
            final Filter chain = context.getBean("springSecurityFilterChain", Filter.class);
            chain.doFilter(login, new MockHttpServletResponse(), (req, res) -> { });
            final var secured = request("GET", "/secured");
            if (login.getSession(false) != null) {
                assertThat(login.getSession(false)
                        .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)).isNull();
                secured.setSession(login.getSession(false));
            }
            final var reached = new AtomicBoolean();
            chain.doFilter(secured, new MockHttpServletResponse(), (req, res) -> reached.set(true));
            assertThat(reached).isFalse();
        });
    }

    @Test
    void shouldValidateIssuedJwtWithoutLocalUserDatabase() {
        runner().run(context -> {
            assertThat(context).hasNotFailed();
            final var tokens = context.getBean(JwtTokenProvider.class);
            final String access = tokens.createToken(SecurityUser.withUsername("remote-user").id("7")
                    .tenant("t001").password("unused").roles().build());
            context.getBean(JwtStorage.class).setAccessToken(tokens.getAccessCacheKey(access), "remote-user");
            when(context.getBean(StorageTemplate.class).opsForString()
                    .hasKey(tokens.getAccessCacheKey(access))).thenReturn(true);
            final var secured = request("GET", "/secured");
            secured.addHeader("Authorization", "Bearer " + access);
            final var reached = new AtomicBoolean();
            context.getBean("springSecurityFilterChain", Filter.class).doFilter(
                    secured, new MockHttpServletResponse(), (req, res) -> reached.set(true));
            assertThat(reached).isTrue();
        });
    }

    @Test
    void shouldIgnorePreviouslyAuthenticatedSessionWithoutJwt() {
        runner().run(context -> {
            final var user = SecurityUser.withUsername("root").password("unused").roles().build();
            final var session = new MockHttpSession();
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    new SecurityContextImpl(UsernamePasswordAuthenticationToken.authenticated(
                            user, null, user.getAuthorities())));
            final var secured = request("GET", "/secured");
            secured.setSession(session);
            final var reached = new AtomicBoolean();
            final var response = new MockHttpServletResponse();
            context.getBean("springSecurityFilterChain", Filter.class)
                    .doFilter(secured, response, (req, res) -> reached.set(true));
            assertThat(reached).isFalse();
            assertThat(response.getStatus()).isEqualTo(401);
        });
    }

    private WebApplicationContextRunner runner() {
        final byte[] secret = new byte[192];
        new SecureRandom().nextBytes(secret);
        return new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(
                        SecurityAutoConfiguration.class, WebSecurityAutoConfiguration.class,
                        JwtAuthAutoConfiguration.class))
                .withPropertyValues("security.context.enabled-access=true", "security.jwt-auth.validation=true",
                        "security.jwt-auth.base64-secret=" + Base64.getEncoder().encodeToString(secret))
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withBean(StorageTemplate.class, () -> mock(StorageTemplate.class, RETURNS_DEEP_STUBS))
                .withBean("mvcHandlerMappingIntrospector", HandlerMappingIntrospector.class,
                        HandlerMappingIntrospector::new);
    }

    private MockHttpServletRequest request(String method, String path) {
        final var request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        return request;
    }
}
