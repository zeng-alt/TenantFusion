package com.github.zeng.alt.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zeng.alt.security.api.LoginHelper;
import com.github.zeng.alt.security.api.LoginResponse;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.api.UserContextHolder;
import com.github.zeng.alt.storage.StorageTemplate;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.Duration;
import java.util.Map;

@CommonsLog
@RequiredArgsConstructor
public class JwtLoginHelper implements LoginHelper {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final StorageTemplate storageTemplate;
    private final JwtProperties jwtProperties;
    private final ObjectMapper objectMapper;

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
            Cookie cookie = new Cookie(jwtProperties.getRefreshCookieName(), String.valueOf(attributes.get("refreshToken")));
            cookie.setHttpOnly(true);
            cookie.setSecure(request.isSecure());
            cookie.setPath(jwtProperties.getRefreshCookiePath());
            cookie.setMaxAge(jwtProperties.getRememberMeExpiration().intValue());
            response.addCookie(cookie);
        }
        return reset;
    }

    @Override
    public LoginResponse reset(SecurityUser user, boolean rememberMe) {
        String token = jwtTokenProvider.createToken(user);
        String cacheKey = jwtTokenProvider.getCacheKey(token);
        storageTemplate.opsForString().set(
                cacheKey,
                user.getUsername(),
                Duration.ofSeconds(jwtProperties.getExpiration())
        );
        LoginResponse loginResponse = LoginResponse.ofUser(user);
        loginResponse.attribute("accessToken", token);

        if (rememberMe) {
            String refreshToken = jwtTokenProvider.createRefreshToken(user);
            String refreshCacheKey = jwtTokenProvider.getRefreshCacheKey(refreshToken);
            storageTemplate.opsForString().set(
                    refreshCacheKey,
                    user.getUsername(),
                    Duration.ofSeconds(jwtProperties.getRememberMeExpiration())
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
        String cacheKey = jwtTokenProvider.getCacheKey(jwt);
        if (cacheKey != null) {
            storageTemplate.opsForString().set(cacheKey, user.getUsername(), Duration.ofSeconds(jwtProperties.getExpiration()));
        }

        LoginResponse response = LoginResponse.ofUser(user)
                .attribute("accessToken", jwt)
                .attribute("tokenType", jwtProperties.getTokenType())
                .attribute("expiresIn", jwtProperties.getExpiration());

        if (rememberMe) {
            String refreshToken = jwtTokenProvider.createRefreshToken(user);
            String refreshCacheKey = jwtTokenProvider.getRefreshCacheKey(refreshToken);
            if (refreshCacheKey != null) {
                storageTemplate.opsForString().set(
                        refreshCacheKey,
                        user.getUsername(),
                        Duration.ofSeconds(jwtProperties.getRememberMeExpiration())
                );
            }
            storeUserInfo(user);
            response.attribute("refreshToken", refreshToken);
            response.attribute("refreshExpiresIn", jwtProperties.getRememberMeExpiration());
        }

        return response;
    }

    private void storeUserInfo(SecurityUser user) {
        try {
            Map<String, Object> userInfo = Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "tenant", user.getTenant() != null ? user.getTenant() : "",
                    "roles", user.getRoles().stream().map(a -> a.getAuthority()).toList(),
                    "currentRole", user.getCurrentRole() != null ? user.getCurrentRole().getAuthority() : ""
            );
            String json = objectMapper.writeValueAsString(userInfo);
            storageTemplate.opsForString().set(
                    jwtTokenProvider.getUserCacheKey(user.getId()),
                    json,
                    Duration.ofSeconds(jwtProperties.getRememberMeExpiration())
            );
        } catch (Exception e) {
            log.warn("Failed to store user info for refresh", e);
        }
    }

    @Override
    public void logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith(jwtProperties.getTokenType())) {
            return;
        }
        String token = authHeader.substring(7);
        String cacheKey = jwtTokenProvider.getCacheKey(token);
        if (cacheKey != null) {
            storageTemplate.delete(cacheKey);
        }
        cleanRefreshTokens(token);
    }

    private void cleanRefreshTokens(String accessToken) {
        try {
            String tokenId = jwtTokenProvider.getTokenId(accessToken);
            if (tokenId != null) {
                String userId = tokenId.contains(":") ? tokenId.substring(0, tokenId.indexOf(':')) : tokenId;
                storageTemplate.opsForString().deleteByPattern(JwtTokenProvider.REFRESH_CACHE_KEY_PREFIX + userId + ":*");
                storageTemplate.delete(jwtTokenProvider.getUserCacheKey(userId));
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public SecurityUser getCurrentUser() {
        return UserContextHolder.getSecurityUser();
    }
}
