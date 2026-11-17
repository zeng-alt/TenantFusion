package com.github.zeng.alt.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zeng.alt.log.LoginInfoEvent;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import java.util.List;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 登录成功和失败均发布原始 IP，供日志组件定位和持久化。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class JwtLoginLogTest {
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void publishesClientIpForSuccessfulLogins() throws Exception {
        final var publisher = mock(ApplicationEventPublisher.class);
        final var tokens = mock(JwtTokenProvider.class);
        final var user = SecurityUser.withUsername("alice").id("1").tenant("tenant-user").build();
        when(tokens.createToken(user)).thenReturn("test-access-token");
        final var handler = new JwtAuthenticationSuccessHandler(
                tokens,
                mock(JwtStorage.class),
                new ObjectMapper().findAndRegisterModules(),
                new JwtProperties(),
                publisher
        );
        final var request = new MockHttpServletRequest();
        request.setRemoteAddr("113.92.157.29");
        request.addHeader("User-Agent", USER_AGENT);
        TenantContextHolder.setTenantId("tenant-request");
        handler.onAuthenticationSuccess(
                request,
                new MockHttpServletResponse(),
                UsernamePasswordAuthenticationToken.authenticated(user, null, List.of())
        );
        final var event = ArgumentCaptor.forClass(LoginInfoEvent.class);
        verify(publisher).publishEvent(event.capture());
        assertThat(event.getValue().getIp()).isEqualTo("113.92.157.29");
        assertThat(event.getValue().getUserAgent()).isEqualTo(USER_AGENT);
        assertThat(event.getValue().getStatus()).isEqualTo("0");
        assertThat(event.getValue().getTenantId()).isEqualTo("tenant-user");
        assertThat(event.getValue().getLoginTime()).isNotNull();
    }

    @Test
    void publishesClientIpForFailedLogins() throws Exception {
        final var publisher = mock(ApplicationEventPublisher.class);
        final var request = new MockHttpServletRequest();
        request.setRemoteAddr("113.92.157.29");
        request.addHeader("User-Agent", USER_AGENT);
        request.setAttribute(JwtLoginFilter.LOGIN_USERNAME_ATTRIBUTE, "alice");
        TenantContextHolder.setTenantId("tenant-attempt");
        new JwtAuthenticationFailureHandler(new ObjectMapper().findAndRegisterModules(), publisher).onAuthenticationFailure(
                request,
                new MockHttpServletResponse(),
                new BadCredentialsException("bad credentials")
        );
        final var event = ArgumentCaptor.forClass(LoginInfoEvent.class);
        verify(publisher).publishEvent(event.capture());
        assertThat(event.getValue().getIp()).isEqualTo("113.92.157.29");
        assertThat(event.getValue().getUserAgent()).isEqualTo(USER_AGENT);
        assertThat(event.getValue().getUsername()).isEqualTo("alice");
        assertThat(event.getValue().getStatus()).isEqualTo("1");
        assertThat(event.getValue().getTenantId()).isEqualTo("tenant-attempt");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void retainsTheAttemptedTenantForFailedJsonAndFormLogins(boolean json) throws Exception {
        final var publisher = mock(ApplicationEventPublisher.class);
        final var request = new MockHttpServletRequest("POST", "/login");
        request.setServletPath("/login");
        if (json) {
            request.setContentType("application/json");
            request.setContent("{\"account\":\"alice\",\"credential\":\"secret\"}".getBytes(StandardCharsets.UTF_8));
        } else {
            request.addParameter("account", "alice");
            request.addParameter("credential", "secret");
        }
        final var filter = new JwtLoginFilter(new ObjectMapper());
        filter.setUsernameParameter("account");
        filter.setPasswordParameter("credential");
        filter.setAuthenticationManager(authentication -> {
            TenantContextHolder.clear();
            throw new BadCredentialsException("bad credentials");
        });
        filter.setAuthenticationFailureHandler(new JwtAuthenticationFailureHandler(new ObjectMapper(), publisher));
        TenantContextHolder.setTenantId("tenant-attempt");
        filter.doFilter(request, new MockHttpServletResponse(), (req, response) -> { });
        final var event = ArgumentCaptor.forClass(LoginInfoEvent.class);
        verify(publisher).publishEvent(event.capture());
        assertThat(event.getValue().getUsername()).isEqualTo("alice");
        assertThat(event.getValue().getTenantId()).isEqualTo("tenant-attempt");
    }

    @Test
    void programmaticLoginPublishesSuccessAndKeepsTheOriginalAuthenticationFailure() {
        final var publisher = mock(ApplicationEventPublisher.class);
        final var tokens = mock(JwtTokenProvider.class);
        final var user = SecurityUser.withUsername("alice").tenant("tenant-user").build();
        when(tokens.createToken(user)).thenReturn("access-token");
        final var helper = new JwtLoginHelper(
                authentication -> UsernamePasswordAuthenticationToken.authenticated(user, null, List.of()),
                tokens, mock(JwtStorage.class), new JwtProperties(), publisher
        );
        assertThat(helper.login("alice", "secret").getAttributes()).containsEntry("accessToken", "access-token");
        final var event = ArgumentCaptor.forClass(LoginInfoEvent.class);
        verify(publisher).publishEvent(event.capture());
        assertThat(event.getValue().getTenantId()).isEqualTo("tenant-user");
        final var rejection = new BadCredentialsException("bad credentials");
        final var unavailable = new JwtLoginHelper(
                authentication -> { throw rejection; }, tokens, mock(JwtStorage.class), new JwtProperties(),
                ignored -> { throw new IllegalStateException("audit unavailable"); }
        );
        assertThatThrownBy(() -> unavailable.login("alice", "secret")).isSameAs(rejection);
    }

    @Test
    void auditFailuresDoNotChangeSuccessfulOrFailedHttpResponses() throws Exception {
        final ApplicationEventPublisher unavailable = event -> { throw new IllegalStateException("database offline"); };
        final var user = SecurityUser.withUsername("alice").build();
        final var tokens = mock(JwtTokenProvider.class);
        when(tokens.createToken(user)).thenReturn("access-token");
        final var mapper = new ObjectMapper().findAndRegisterModules();
        final var success = new MockHttpServletResponse();
        new JwtAuthenticationSuccessHandler(tokens, mock(JwtStorage.class), mapper, new JwtProperties(), unavailable)
                .onAuthenticationSuccess(new MockHttpServletRequest(), success,
                        UsernamePasswordAuthenticationToken.authenticated(user, null, List.of()));
        assertThat(success.getStatus()).isEqualTo(200);
        assertThat(success.getContentAsString()).contains("access-token");
        final var failure = new MockHttpServletResponse();
        new JwtAuthenticationFailureHandler(mapper, unavailable).onAuthenticationFailure(
                new MockHttpServletRequest(), failure, new BadCredentialsException("bad credentials")
        );
        assertThat(failure.getStatus()).isEqualTo(401);
        assertThat(failure.getContentAsString()).contains("bad credentials");
    }
}
