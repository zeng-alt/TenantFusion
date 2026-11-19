package com.github.zeng.alt.security.cookie;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.ObjectMapper;
import com.github.zeng.alt.log.LoginInfoEvent;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.core.properties.UsernameLoginProperties;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Cookie 登录事件、租户归属和 JSON/表单失败用户名采集契约。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class CookieLoginLogTest {
    private static final String USER_AGENT = "test-agent";

    @Test
    void rejectsMalformedJsonAsAnAuthenticationFailure() {
        final var request = new MockHttpServletRequest();
        request.setContentType("application/json");
        request.setContent("{".getBytes(StandardCharsets.UTF_8));
        final var filter = new CookieLoginFilter(JsonMapper.builder().build());
        assertThatThrownBy(() -> filter.attemptAuthentication(request, new MockHttpServletResponse()))
                .isInstanceOf(AuthenticationServiceException.class).hasCauseInstanceOf(JacksonException.class);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void recordsSuccessfulLoginUsingTheAuthenticatedTenant() {
        runner(authentication -> authentication).run(context -> {
            final SecurityUser user = SecurityUser.withUsername("alice").id("7").tenant("tenant-user").build();
            TenantContextHolder.setTenantId("tenant-request");
            context.getBean(CookieAuthenticationSuccessHandler.class).onAuthenticationSuccess(
                    request(),
                    new MockHttpServletResponse(),
                    UsernamePasswordAuthenticationToken.authenticated(user, null, List.of())
            );
            final LoginInfoEvent event = context.getBean(Records.class).events.getFirst();
            assertThat(event.getUsername()).isEqualTo("alice");
            assertThat(event.getTenantId()).isEqualTo("tenant-user");
            assertThat(event.getStatus()).isEqualTo("0");
            assertThat(event.getIp()).isEqualTo("113.92.157.29");
            assertThat(event.getUserAgent()).isEqualTo(USER_AGENT);
            assertThat(event.getLoginTime()).isNotNull();
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void recordsFailedJsonAndFormLoginsWithCustomParameterNames(boolean json) {
        runner(authentication -> {
            TenantContextHolder.clear();
            throw new BadCredentialsException("bad credentials");
        }).run(context -> {
            TenantContextHolder.setTenantId("tenant-attempt");
            final var request = request();
            if (json) {
                request.setContentType("application/json");
                request.setContent("{\"account\":\"alice\",\"credential\":\"secret\"}".getBytes(StandardCharsets.UTF_8));
            } else {
                request.addParameter("account", "alice");
                request.addParameter("credential", "secret");
            }
            final var filter = new CookieLoginFilter(context.getBean(ObjectMapper.class));
            filter.setUsernameParameter("account");
            filter.setPasswordParameter("credential");
            filter.setAuthenticationManager(context.getBean(AuthenticationManager.class));
            filter.setAuthenticationFailureHandler(context.getBean(CookieAuthenticationFailureHandler.class));
            filter.doFilter(request, new MockHttpServletResponse(), (req, response) -> { });
            final LoginInfoEvent event = context.getBean(Records.class).events.getFirst();
            assertThat(event.getUsername()).isEqualTo("alice");
            assertThat(event.getTenantId()).isEqualTo("tenant-attempt");
            assertThat(event.getStatus()).isEqualTo("1");
            assertThat(event.getMessage()).isEqualTo("bad credentials");
            assertThat(event.getIp()).isEqualTo("113.92.157.29");
            assertThat(event.getUserAgent()).isEqualTo(USER_AGENT);
            assertThat(event.getArgs()).isNull();
        });
    }

    @Test
    void autoConfiguredLoginHelperPublishesExactlyOneSuccessEvent() {
        final SecurityUser user = SecurityUser.withUsername("alice").tenant("tenant-user").build();
        runner(authentication -> UsernamePasswordAuthenticationToken.authenticated(user, null, List.of()))
                .run(context -> {
                    final var response = context.getBean(CookieLoginHelper.class).login("alice", "secret");
                    assertThat(response.getAttributes()).containsEntry("sessionId", "session-id");
                    assertThat(context.getBean(Records.class).events).singleElement().satisfies(event -> {
                        assertThat(event.getTenantId()).isEqualTo("tenant-user");
                        assertThat(event.getStatus()).isEqualTo("0");
                    });
                });
    }

    @Test
    void autoConfiguredLoginHelperRetainsTheFailureAndTenant() {
        final var rejection = new BadCredentialsException("bad credentials");
        runner(authentication -> {
            TenantContextHolder.clear();
            throw rejection;
        }).run(context -> {
            TenantContextHolder.setTenantId("tenant-attempt");
            assertThatThrownBy(() -> context.getBean(CookieLoginHelper.class).login("alice", "secret"))
                    .isSameAs(rejection);
            assertThat(context.getBean(Records.class).events).singleElement().satisfies(event -> {
                assertThat(event.getTenantId()).isEqualTo("tenant-attempt");
                assertThat(event.getStatus()).isEqualTo("1");
            });
        });
    }

    @Test
    void auditFailuresDoNotChangeSuccessfulOrFailedHttpResponses() {
        runner(authentication -> authentication).withBean(RejectingListener.class).run(context -> {
            final SecurityUser user = SecurityUser.withUsername("alice").build();
            final var success = new MockHttpServletResponse();
            context.getBean(CookieAuthenticationSuccessHandler.class).onAuthenticationSuccess(
                    request(), success, UsernamePasswordAuthenticationToken.authenticated(user, null, List.of())
            );
            assertThat(success.getStatus()).isEqualTo(200);
            assertThat(success.getCookie(context.getBean(CookieAuthProperties.class).getCookieName())).isNotNull();
            final var failure = new MockHttpServletResponse();
            context.getBean(CookieAuthenticationFailureHandler.class).onAuthenticationFailure(
                    request(), failure, new BadCredentialsException("bad credentials")
            );
            assertThat(failure.getStatus()).isEqualTo(401);
            assertThat(failure.getContentAsString()).contains("bad credentials");
        });
    }

    private MockHttpServletRequest request() {
        final var request = new MockHttpServletRequest("POST", "/login");
        request.setServletPath("/login");
        request.setRemoteAddr("113.92.157.29");
        request.addHeader("User-Agent", USER_AGENT);
        return request;
    }

    private WebApplicationContextRunner runner(AuthenticationManager authenticationManager) {
        final SessionManager sessions = mock(SessionManager.class);
        when(sessions.createSession(org.mockito.ArgumentMatchers.any())).thenReturn("session-id");
        return new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(CookieAuthAutoConfiguration.class))
                .withPropertyValues("security.cookie-auth.authentication=true", "security.cookie-auth.validation=true")
                .withBean(ObjectMapper.class, () -> JsonMapper.builder().findAndAddModules().build())
                .withBean(SessionManager.class, () -> sessions)
                .withBean(AuthenticationManager.class, () -> authenticationManager)
                .withBean(UserDetailsService.class, () -> mock(UserDetailsService.class))
                .withBean(UsernameLoginProperties.class, UsernameLoginProperties::new)
                .withUserConfiguration(Fixtures.class);
    }

    /**
     * 故障日志接收方。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    static class RejectingListener {
        @EventListener
        public void reject(LoginInfoEvent event) { throw new IllegalStateException("database unavailable"); }
    }

    /**
     * 测试事件收集器装配。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    @Configuration(proxyBeanMethods = false)
    static class Fixtures {
        @Bean Records records() { return new Records(); }
    }

    /**
     * 捕获审计事件。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    static class Records {
        final List<LoginInfoEvent> events = new ArrayList<>();
        @EventListener
        public void login(LoginInfoEvent event) { events.add(event); }
    }
}
