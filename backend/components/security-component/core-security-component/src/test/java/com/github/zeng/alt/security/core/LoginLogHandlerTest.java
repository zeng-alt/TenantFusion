package com.github.zeng.alt.security.core;

import com.github.zeng.alt.log.LoginInfoEvent;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.core.web.support.LoginLogHandler;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 认证边界的日志故障隔离和租户快照契约。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class LoginLogHandlerTest {
    @AfterEach
    void clearContext() {
        RequestContextHolder.resetRequestAttributes();
        TenantContextHolder.clear();
    }

    @Test
    void keepsTheOriginalFailureAndTheTenantCapturedBeforeAuthentication() {
        final List<Object> events = new ArrayList<>();
        final var handler = new LoginLogHandler(events::add);
        final var request = new MockHttpServletRequest();
        request.setRemoteAddr("113.92.157.29");
        request.addHeader("User-Agent", "test-agent");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        TenantContextHolder.setTenantId("tenant-a");
        final var rejection = new BadCredentialsException("bad credentials");
        assertThatThrownBy(() -> handler.authenticate(authentication -> {
            TenantContextHolder.clear();
            throw rejection;
        }, "alice", "secret")).isSameAs(rejection);
        final LoginInfoEvent event = (LoginInfoEvent) events.getFirst();
        assertThat(event.getUsername()).isEqualTo("alice");
        assertThat(event.getTenantId()).isEqualTo("tenant-a");
        assertThat(event.getIp()).isEqualTo("113.92.157.29");
        assertThat(event.getUserAgent()).isEqualTo("test-agent");
        assertThat(event.getMessage()).isEqualTo("bad credentials");
        assertThat(event.getStatus()).isEqualTo("1");
        assertThat(event.getLoginTime()).isNotNull();
        assertThat(event.getArgs()).isNull();
    }

    @Test
    void capturesProgrammaticSuccessWithoutAnHttpRequest() {
        final List<Object> events = new ArrayList<>();
        final var user = SecurityUser.withUsername("alice").tenant("tenant-user").build();
        TenantContextHolder.setTenantId("tenant-request");
        assertThat(new LoginLogHandler(events::add).authenticate(
                authentication -> UsernamePasswordAuthenticationToken.authenticated(user, null, List.of()),
                "alice",
                "secret"
        )).isSameAs(user);
        final LoginInfoEvent event = (LoginInfoEvent) events.getFirst();
        assertThat(event.getTenantId()).isEqualTo("tenant-user");
        assertThat(event.getIp()).isEmpty();
        assertThat(event.getUserAgent()).isEmpty();
        assertThat(event.getStatus()).isEqualTo("0");
    }

    @Test
    void fallsBackToTheRequestTenantWhenThePrincipalHasNoTenant() {
        final List<Object> events = new ArrayList<>();
        final var request = new MockHttpServletRequest();
        TenantContextHolder.setTenantId("tenant-request");
        LoginLogHandler.captureAttempt(request, "alice");
        TenantContextHolder.setTenantId("tenant-changed");
        new LoginLogHandler(events::add).success(request, SecurityUser.withUsername("alice").build());
        assertThat(((LoginInfoEvent) events.getFirst()).getTenantId()).isEqualTo("tenant-request");
        assertThat(TenantContextHolder.getTenantId()).isEqualTo("tenant-changed");
    }

    @Test
    void recordsUnknownTenantAsEmptyWithoutReadingUnresolvedHeaders() {
        final List<Object> events = new ArrayList<>();
        final var request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "unresolved");
        request.addParameter("username", "alice");
        new LoginLogHandler(events::add).failure(request, new BadCredentialsException("bad credentials"));
        final LoginInfoEvent event = (LoginInfoEvent) events.getFirst();
        assertThat(event.getUsername()).isEqualTo("alice");
        assertThat(event.getTenantId()).isEmpty();
    }

    @Test
    void doesNotReplaceAuthenticationResultsOrFailuresWhenTheAuditSinkFails() {
        final ApplicationEventPublisher unavailable = event -> { throw new IllegalStateException("database offline"); };
        final var handler = new LoginLogHandler(unavailable);
        final var user = SecurityUser.withUsername("alice").build();
        assertThat(handler.authenticate(
                authentication -> UsernamePasswordAuthenticationToken.authenticated(user, null, List.of()),
                "alice",
                "secret"
        )).isSameAs(user);
        final var rejection = new BadCredentialsException("bad credentials");
        assertThatThrownBy(() -> handler.authenticate(authentication -> { throw rejection; }, "alice", "secret"))
                .isSameAs(rejection);
    }
}
