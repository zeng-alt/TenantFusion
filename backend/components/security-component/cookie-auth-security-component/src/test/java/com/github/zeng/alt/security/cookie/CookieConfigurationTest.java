package com.github.zeng.alt.security.cookie;

import tools.jackson.databind.ObjectMapper;
import com.github.zeng.alt.security.api.SecurityUser;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证会话 Cookie 的签发和清除共享属性，HTTPS 默认携带 Secure。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class CookieConfigurationTest {
    @Test
    void shouldUseSameCookieScopeWhenIssuingAndClearingSession() throws Exception {
        final var properties = new CookieAuthProperties();
        properties.setCookieName("CUSTOM_SESSION");
        properties.setCookiePath("/api");
        properties.setCookieDomain("example.test");
        final var sessions = mock(SessionManager.class);
        final SecurityUser user = SecurityUser.withUsername("review").id("7").password("unused").build();
        when(sessions.createSession(user)).thenReturn("session-id");
        final var request = new MockHttpServletRequest();
        request.setSecure(true);
        final var response = new MockHttpServletResponse();
        final ObjectMapper mapper = JsonMapper.builder().build();
        new CookieAuthenticationSuccessHandler(sessions, mapper, properties).onAuthenticationSuccess(request, response,
                UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
        final var cookie = response.getCookie("CUSTOM_SESSION");
        assertThat(cookie).isNotNull();
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getAttribute("SameSite")).isEqualTo("Lax");
        assertThat(cookie.getPath()).isEqualTo("/api");
        assertThat(cookie.getDomain()).isEqualTo("example.test");
        request.setCookies(cookie);
        final var logout = new MockHttpServletResponse();
        new CookieLogoutHandler(sessions, properties).logout(request, logout, null);
        verify(sessions).removeSession("session-id");
        final var cleared = logout.getCookie("CUSTOM_SESSION");
        assertThat(cleared).isNotNull();
        assertThat(cleared.getMaxAge()).isZero();
        assertThat(cleared.getPath()).isEqualTo(cookie.getPath());
        assertThat(cleared.getDomain()).isEqualTo(cookie.getDomain());
        assertThat(cleared.getAttribute("SameSite")).isEqualTo(cookie.getAttribute("SameSite"));
    }

    @Test
    void shouldKeepLocalHttpCookieAndExplicitAttributesConfigurable() {
        final var properties = new CookieAuthProperties();
        final var request = new MockHttpServletRequest();
        assertThat(CookieHelper.create(properties, request, "session", 10).getSecure()).isFalse();
        properties.setCookieSecure(true);
        properties.setCookieSameSite("Strict");
        properties.setCookieHttpOnly(false);
        final var cookie = CookieHelper.create(properties, request, "session", 10);
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.isHttpOnly()).isFalse();
        assertThat(cookie.getAttribute("SameSite")).isEqualTo("Strict");
    }
}
