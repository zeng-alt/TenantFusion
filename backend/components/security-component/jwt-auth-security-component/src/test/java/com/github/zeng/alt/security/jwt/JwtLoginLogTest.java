package com.github.zeng.alt.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zeng.alt.log.LoginInfoEvent;
import com.github.zeng.alt.security.api.SecurityUser;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    void publishesClientIpForSuccessfulLogins() throws Exception {
        final var publisher = mock(ApplicationEventPublisher.class);
        final var tokens = mock(JwtTokenProvider.class);
        final var user = SecurityUser.withUsername("alice").id("1").build();
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
    }

    @Test
    void publishesClientIpForFailedLogins() throws Exception {
        final var publisher = mock(ApplicationEventPublisher.class);
        final var request = new MockHttpServletRequest();
        request.setRemoteAddr("113.92.157.29");
        request.addHeader("User-Agent", USER_AGENT);
        request.setAttribute(JwtLoginFilter.LOGIN_USERNAME_ATTRIBUTE, "alice");
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
    }
}
