package com.github.zeng.alt.security.jwt;

import tools.jackson.databind.json.JsonMapper;
import com.github.zeng.alt.security.api.SecurityUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import jakarta.servlet.http.Cookie;

import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.ArgumentMatchers.any;

/**
 * 验证 JWT 两条签发路径遵守相同的 Refresh Cookie 配置。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class JwtCookieConfigurationTest {
    private final JwtProperties properties = new JwtProperties();
    private final JwtStorage storage = mock(JwtStorage.class);
    private final SecurityUser user = SecurityUser.withUsername("review").id("7")
            .tenant("t001").password("unused").build();
    private JwtTokenProvider tokens;

    @BeforeEach
    void setUp() {
        final byte[] secret = new byte[192];
        new SecureRandom().nextBytes(secret);
        properties.setBase64Secret(Base64.getEncoder().encodeToString(secret));
        properties.setRefreshCookieName("CUSTOM_REFRESH");
        properties.setRefreshCookiePath("/api");
        properties.setRefreshCookieSameSite("Strict");
        tokens = new JwtTokenProvider(properties);
    }

    @Test
    void shouldApplySameSiteToResetCookie() {
        final var response = new MockHttpServletResponse();
        new JwtLoginHelper(mock(AuthenticationManager.class), tokens, storage, properties)
                .reset(user, true, secureRequest(), response);
        assertCookie(response.getCookie(properties.getRefreshCookieName()));
    }

    @Test
    void shouldApplySameSiteToLoginCookie() throws Exception {
        final var request = secureRequest();
        request.setParameter("rememberMe", "true");
        final var response = new MockHttpServletResponse();
        new JwtAuthenticationSuccessHandler(tokens, storage, JsonMapper.builder().build(), properties,
                mock(ApplicationEventPublisher.class)).onAuthenticationSuccess(request, response,
                        UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
        assertCookie(response.getCookie(properties.getRefreshCookieName()));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldRevokeRefreshCredentialOnLogout(boolean header) {
        final String access = tokens.createToken(user);
        final String refresh = tokens.createRefreshToken(user);
        properties.setRefreshTokenHeader("X-Custom-Refresh");
        final var request = secureRequest();
        request.addHeader("Authorization", "Bearer " + access);
        if (header) {
            request.addHeader(properties.getRefreshTokenHeader(), refresh);
        } else {
            request.setCookies(new Cookie(properties.getRefreshCookieName(), refresh));
        }
        new JwtLoginHelper(mock(AuthenticationManager.class), tokens, storage, properties).logout(request);
        verify(storage).removeToken(tokens.getAccessCacheKey(access));
        verify(storage).removeToken(tokens.getRefreshCacheKey(refresh));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldPreserveProgrammaticLoginResponseAndCacheLifetimes(boolean rememberMe) {
        final var manager = mock(AuthenticationManager.class);
        when(manager.authenticate(any())).thenReturn(
                UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
        final var response = new JwtLoginHelper(manager, tokens, storage, properties)
                .login("review", "password", rememberMe);
        final var attributes = response.getAttributes();
        final String access = (String) attributes.get("accessToken");
        assertThat(tokens.parseAccessToken(access).result())
                .isEqualTo(JwtTokenProvider.TokenValidationResult.VALID);
        assertThat(attributes.get("expiresIn")).isEqualTo(properties.getExpiration());
        verify(storage).setAccessToken(tokens.getAccessCacheKey(access), "review");
        assertThat(attributes.containsKey("refreshToken")).isEqualTo(rememberMe);
        if (rememberMe) {
            verify(storage).setRefreshToken(tokens.getRefreshCacheKey((String) attributes.get("refreshToken")), "review");
            assertThat(attributes.get("refreshExpiresIn")).isEqualTo(properties.getRememberMeExpiration());
        }
    }

    @Test
    void shouldRevokeOnlyCurrentTenantUserCredentials() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
        new JwtLoginHelper(mock(AuthenticationManager.class), tokens, storage, properties).logout("7");
        verify(storage).removeAllToken("jwt:token:t001:7");
        verify(storage).removeAllToken("jwt:refresh:t001:7");
    }

    @Test
    void shouldNotHideRefreshRevocationFailure() {
        final String access = tokens.createToken(user);
        final String refresh = tokens.createRefreshToken(user);
        final var request = secureRequest();
        request.addHeader("Authorization", "Bearer " + access);
        request.setCookies(new Cookie(properties.getRefreshCookieName(), refresh));
        doThrow(new IllegalStateException("storage unavailable")).when(storage)
                .removeToken(tokens.getRefreshCacheKey(refresh));
        assertThatThrownBy(() -> new JwtLoginHelper(mock(AuthenticationManager.class), tokens, storage, properties)
                .logout(request)).isInstanceOf(IllegalStateException.class).hasMessage("storage unavailable");
    }

    @Test
    void shouldNotChangeCredentialsWithoutBearerHeader() {
        new JwtLoginHelper(mock(AuthenticationManager.class), tokens, storage, properties).logout(secureRequest());
        verifyNoInteractions(storage);
    }

    private MockHttpServletRequest secureRequest() {
        final var request = new MockHttpServletRequest();
        request.setSecure(true);
        return request;
    }

    private void assertCookie(Cookie cookie) {
        assertThat(cookie).isNotNull();
        assertThat(cookie.getAttribute("SameSite")).isEqualTo("Strict");
        assertThat(cookie.getPath()).isEqualTo("/api");
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getMaxAge()).isEqualTo(properties.getRememberMeExpiration().intValue());
    }
}
