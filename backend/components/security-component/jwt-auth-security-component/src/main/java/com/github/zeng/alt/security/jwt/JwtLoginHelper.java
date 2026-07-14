package com.github.zeng.alt.security.jwt;

import com.github.zeng.alt.security.api.LoginHelper;
import com.github.zeng.alt.security.api.LoginResponse;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.api.UserContextHolder;
import com.github.zeng.alt.storage.StorageTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.Duration;

/**
 * JWT 登录认证实现.
 *
 * @author zengJiaJun
 * @version 1.0
 * @since 2024年10月07日
 */
@RequiredArgsConstructor
public class JwtLoginHelper implements LoginHelper {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final StorageTemplate storageTemplate;
    private final long expiration;
    private final long rememberMeExpiration;

    @Override
    public String name() {
        return "jwt";
    }

    @Override
    public LoginResponse login(String username, String password) {
        return login(username, password, false);
    }

    public LoginResponse login(String username, String password, boolean rememberMe) {
        UsernamePasswordAuthenticationToken authRequest =
                UsernamePasswordAuthenticationToken.unauthenticated(username, password);
        Authentication authenticated = authenticationManager.authenticate(authRequest);
        SecurityUser user = (SecurityUser) authenticated.getPrincipal();

        String jwt = jwtTokenProvider.createToken(user);
        String cacheKey = jwtTokenProvider.getCacheKey(jwt);
        if (cacheKey != null) {
            storageTemplate.opsForString().set(cacheKey, user.getUsername(), Duration.ofSeconds(expiration));
        }

        LoginResponse response = LoginResponse.success(user)
                .attribute("accessToken", jwt)
                .attribute("tokenType", "Bearer")
                .attribute("expiresIn", expiration);

        if (rememberMe) {
            String refreshToken = jwtTokenProvider.createRefreshToken(user, rememberMeExpiration);
            String refreshCacheKey = jwtTokenProvider.getRefreshCacheKey(refreshToken);
            if (refreshCacheKey != null) {
                storageTemplate.opsForString().set(
                        refreshCacheKey,
                        user.getUsername(),
                        Duration.ofSeconds(rememberMeExpiration)
                );
            }
            response.attribute("refreshToken", refreshToken);
            response.attribute("refreshExpiresIn", rememberMeExpiration);
        }

        return response;
    }

    @Override
    public void logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }
        String token = authHeader.substring(7);
        String cacheKey = jwtTokenProvider.getCacheKey(token);
        if (cacheKey != null) {
            storageTemplate.delete(cacheKey);
        }
        // 同时清理该用户的 refreshToken 缓存
        cleanRefreshTokens(token);
    }

    private void cleanRefreshTokens(String accessToken) {
        try {
            String tokenId = jwtTokenProvider.getTokenId(accessToken);
            if (tokenId != null) {
                String userId = tokenId.contains(":") ? tokenId.substring(0, tokenId.indexOf(':')) : tokenId;
                storageTemplate.opsForString().deleteByPattern(JwtTokenProvider.REFRESH_CACHE_KEY_PREFIX + userId + ":*");
            }
        } catch (Exception ignored) {
            // 若 token 已过期无法解析，直接跳过
        }
    }

    @Override
    public SecurityUser getCurrentUser() {
        return UserContextHolder.getSecurityUser();
    }
}
