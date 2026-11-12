package com.github.zeng.alt.security.cookie;

import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.api.UserContextHolder;
import com.github.zeng.alt.storage.GuavaCacheStringOperations;
import com.github.zeng.alt.storage.StorageTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.mock.web.MockHttpServletRequest;
import jakarta.servlet.http.Cookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

/**
 * 验证按用户撤销全部 Cookie 会话，并隔离其他租户和用户。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class SessionManagerTest {
    @AfterEach
    void clearContext() {
        UserContextHolder.clear();
    }

    @Test
    void userWideLogoutRevokesEverySessionOnlyInCurrentTenant() {
        final var storage = mock(StorageTemplate.class);
        when(storage.opsForString()).thenReturn(new GuavaCacheStringOperations());
        final var sessions = new SessionManager(storage, 86400);
        final var firstUser = user("7", "tenant:a");
        final String first = sessions.createSession(firstUser);
        final String second = sessions.createSession(firstUser);
        final String otherUser = sessions.createSession(user("70", "tenant:a"));
        final String otherTenant = sessions.createSession(user("7", "tenant:b"));
        UserContextHolder.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                firstUser, null, firstUser.getAuthorities()));
        assertThat(sessions.getSession(first)).isNotNull();
        new CookieLoginHelper(mock(AuthenticationManager.class), sessions, new CookieAuthProperties()).logout("7");
        assertThat(sessions.getSession(first)).isNull();
        assertThat(sessions.getSession(second)).isNull();
        assertThat(sessions.getSession(otherUser)).isNotNull();
        assertThat(sessions.getSession(otherTenant)).isNotNull();
        final String replacement = sessions.createSession(firstUser);
        assertThat(sessions.getSession(replacement)).isNotNull();
    }

    @Test
    void singleSessionLogoutKeepsOtherSessionsAndLegacySessionsRequireLogin() {
        final var storage = mock(StorageTemplate.class);
        final var cache = new GuavaCacheStringOperations();
        when(storage.opsForString()).thenReturn(cache);
        final var sessions = new SessionManager(storage, 86400);
        final var user = user("7", "tenant:a");
        cache.set("session:legacy-id", user);
        assertThat(sessions.getSession("legacy-id")).isNull();
        final String first = sessions.createSession(user);
        final String second = sessions.createSession(user);
        sessions.removeSession(first);
        assertThat(sessions.getSession(first)).isNull();
        assertThat(sessions.getSession(second)).isNotNull();
    }

    @Test
    void supportsSingleTenantSessionsAndEncodesPatternCharacters() {
        final var storage = mock(StorageTemplate.class);
        when(storage.opsForString()).thenReturn(new GuavaCacheStringOperations());
        final var sessions = new SessionManager(storage, 86400);
        final String first = sessions.createSession(user("7:*", null));
        final String other = sessions.createSession(user("7:any", null));
        assertThat(sessions.removeUserSessions("7:*", null)).isEqualTo(1);
        assertThat(sessions.getSession(first)).isNull();
        assertThat(sessions.getSession(other)).isNotNull();
    }

    @Test
    void loginHelperCreatesAndLogsOutCurrentSession() {
        final var storage = mock(StorageTemplate.class);
        when(storage.opsForString()).thenReturn(new GuavaCacheStringOperations());
        final var sessions = new SessionManager(storage, 86400);
        final var manager = mock(AuthenticationManager.class);
        final var user = user("7", "a");
        final var authentication = UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities());
        when(manager.authenticate(any())).thenReturn(authentication);
        final var helper = new CookieLoginHelper(manager, sessions, new CookieAuthProperties());
        final var login = helper.login("review", "unused");
        final String sessionId = (String) login.getAttributes().get("sessionId");
        assertThat(sessions.getSession(sessionId)).isNotNull();
        assertThat(helper.name()).isEqualTo("cookie");
        UserContextHolder.setAuthentication(authentication);
        assertThat(helper.getCurrentUser()).isSameAs(user);
        final var request = new MockHttpServletRequest();
        helper.logout(request);
        request.setCookies(new Cookie("unrelated", "unused"));
        helper.logout(request);
        assertThat(sessions.getSession(sessionId)).isNotNull();
        request.setCookies(new Cookie("SESSION", sessionId));
        helper.logout(request);
        assertThat(sessions.getSession(sessionId)).isNull();
    }

    private SecurityUser user(String id, String tenant) {
        return SecurityUser.withUsername("review").id(id).tenant(tenant).password("unused").build();
    }
}
