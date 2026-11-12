package com.github.zeng.alt.security.jwt;

import com.github.zeng.alt.security.api.LoginHelper;
import com.github.zeng.alt.security.api.LoginResponse;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.api.UserContextHolder;
import io.vavr.control.Option;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Map;

@CommonsLog
@RequiredArgsConstructor
public class JwtLoginHelper implements LoginHelper {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtStorage jwtStorage;
    private final JwtProperties jwtProperties;

    @Override
    public String name() {
        return "jwt";
    }

    @Override
    public LoginResponse login(String username, String password) {
        return login(username, password, false);
    }

    @Override
    public LoginResponse reset(SecurityUser user, boolean rememberMe, HttpServletRequest request, HttpServletResponse response) {
        LoginResponse reset = reset(user, rememberMe);
        Map<String, Object> attributes = reset.getAttributes();
        if (rememberMe) {
            response.addCookie(JwtCookieHelper.create(jwtProperties, request, String.valueOf(attributes.get("refreshToken")),
                    jwtProperties.getRememberMeExpiration().intValue()));
        }
        return reset;
    }

    @Override
    public LoginResponse reset(SecurityUser user, boolean rememberMe) {
        String token = jwtTokenProvider.createToken(user);
        String cacheKey = jwtTokenProvider.getAccessCacheKey(token);
        jwtStorage.setAccessToken(
                cacheKey,
                user.getUsername()
        );
        LoginResponse loginResponse = LoginResponse.ofUser(user);
        loginResponse.attribute("accessToken", token);

        if (rememberMe) {
            String refreshToken = jwtTokenProvider.createRefreshToken(user);
            String refreshCacheKey = jwtTokenProvider.getRefreshCacheKey(refreshToken);
            jwtStorage.setRefreshToken(
                    refreshCacheKey,
                    user.getUsername()
            );
            loginResponse.attribute("refreshToken", refreshToken);
        }
        return loginResponse;
    }

    @Override
    public LoginResponse login(String username, String password, boolean rememberMe) {
        UsernamePasswordAuthenticationToken authRequest =
                UsernamePasswordAuthenticationToken.unauthenticated(username, password);
        Authentication authenticated = authenticationManager.authenticate(authRequest);
        SecurityUser user = (SecurityUser) authenticated.getPrincipal();

        String jwt = jwtTokenProvider.createToken(user);
        String cacheKey = jwtTokenProvider.getAccessCacheKey(jwt);
        if (cacheKey != null) {
            jwtStorage.setAccessToken(cacheKey, user.getUsername());
        }

        LoginResponse response = LoginResponse.ofUser(user)
                .attribute("accessToken", jwt)
                .attribute("tokenType", jwtProperties.getTokenType())
                .attribute("expiresIn", jwtProperties.getExpiration());

        if (rememberMe) {
            String refreshToken = jwtTokenProvider.createRefreshToken(user);
            String refreshCacheKey = jwtTokenProvider.getRefreshCacheKey(refreshToken);
            if (refreshCacheKey != null) {
                jwtStorage.setRefreshToken(
                        refreshCacheKey,
                        user.getUsername()
                );
            }
            response.attribute("refreshToken", refreshToken);
            response.attribute("refreshExpiresIn", jwtProperties.getRememberMeExpiration());
        }

        return response;
    }

    @Override
    public void logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith(jwtProperties.getTokenType())) {
            return;
        }
        String token = authHeader.substring(jwtProperties.getTokenType().length());
        String cacheKey = jwtTokenProvider.getAccessCacheKey(token);
        if (cacheKey != null) {
            jwtStorage.removeToken(cacheKey);
        }
        cleanRefreshTokens(request);
    }

    @Override
    public void logout(String id) {
        String accessCachePrefix = jwtTokenProvider.getAccessCachePrefix(id);
        String refreshCachePrefix = jwtTokenProvider.getRefreshCachePrefix(id);
        jwtStorage.removeAllToken(accessCachePrefix);
        jwtStorage.removeAllToken(refreshCachePrefix);
    }

    private void cleanRefreshTokens(HttpServletRequest request) {
        Option.of(request.getHeader(jwtProperties.getRefreshTokenHeader())).filter(StringUtils::hasText)
                .orElse(() -> Option.of(request.getCookies()).map(Arrays::stream)
                        .flatMap(cookies -> Option.ofOptional(cookies
                                .filter(cookie -> jwtProperties.getRefreshCookieName().equals(cookie.getName()))
                                .map(Cookie::getValue).findFirst())))
                .flatMap(token -> Option.of(jwtTokenProvider.getRefreshCacheKey(token)))
                .peek(jwtStorage::removeToken);
    }

    @Override
    public SecurityUser getCurrentUser() {
        return UserContextHolder.getSecurityUser();
    }
}
