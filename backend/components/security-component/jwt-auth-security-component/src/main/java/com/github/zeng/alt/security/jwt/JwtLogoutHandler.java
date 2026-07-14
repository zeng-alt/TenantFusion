package com.github.zeng.alt.security.jwt;

import com.github.zeng.alt.storage.StorageTemplate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;

/**
 * JWT 登出处理器.
 * <p>
 * 从请求头中提取 Bearer token，将其从缓存中删除实现登出。
 * 同时清理该用户的所有 refreshToken 缓存，实现完整登出。
 * 后续携带该 token 的请求将被 {@link JwtAuthenticationFilter} 拒绝。
 *
 * @author zengJiaJun
 * @version 1.0
 * @since 2024年10月07日
 */
@RequiredArgsConstructor
public class JwtLogoutHandler implements LogoutHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final StorageTemplate storageTemplate;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }
        String token = authHeader.substring(7);
        String cacheKey = jwtTokenProvider.getCacheKey(token);
        if (cacheKey != null) {
            storageTemplate.delete(cacheKey);
        }
        // 清理该用户的 refreshToken 缓存
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
}
