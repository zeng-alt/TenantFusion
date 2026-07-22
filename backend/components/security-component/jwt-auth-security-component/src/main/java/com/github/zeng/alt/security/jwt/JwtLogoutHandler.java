package com.github.zeng.alt.security.jwt;

import com.github.zeng.alt.storage.StorageTemplate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;

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
}
