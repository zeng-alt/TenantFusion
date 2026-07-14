package com.github.zeng.alt.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zeng.alt.api.rest.RestResponse;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.storage.StorageTemplate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JWT 认证成功处理器.
 * <p>
 * 登录成功后生成 JWT token，将其存入缓存（供后续请求验证），
 * 并以 JSON 格式返回 {@code accessToken}、{@code tokenType}、{@code expiresIn}。
 * <p>
 * 当请求参数中 {@code rememberMe=true} 时，额外生成一个长效 refreshToken 返回，
 * 供后续 accessToken 过期时无感续期使用。
 *
 * @author zengJiaJun
 * @version 1.0
 * @since 2024年10月07日
 */
@RequiredArgsConstructor
public class JwtAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final StorageTemplate storageTemplate;
    private final ObjectMapper objectMapper;
    private final long expiration;
    private final long rememberMeExpiration;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
        String token = jwtTokenProvider.createToken(securityUser);
        String cacheKey = jwtTokenProvider.getCacheKey(token);

        // JWT 存入缓存，TTL = token 有效期，用于后续请求校验 / 登出失效
        if (cacheKey != null) {
            storageTemplate.opsForString().set(
                    cacheKey,
                    securityUser.getUsername(),
                    Duration.ofSeconds(expiration)
            );
        }

        Map<String, Object> tokenData = new LinkedHashMap<>();
        tokenData.put("accessToken", token);
        tokenData.put("tokenType", "Bearer");
        tokenData.put("expiresIn", expiration);

        // 记住我：额外生成 refreshToken
        if (isRememberMe(request)) {
            String refreshToken = jwtTokenProvider.createRefreshToken(securityUser, rememberMeExpiration);
            String refreshCacheKey = jwtTokenProvider.getRefreshCacheKey(refreshToken);
            if (refreshCacheKey != null) {
                storageTemplate.opsForString().set(
                        refreshCacheKey,
                        securityUser.getUsername(),
                        Duration.ofSeconds(rememberMeExpiration)
                );
            }
            tokenData.put("refreshToken", refreshToken);
            tokenData.put("refreshExpiresIn", rememberMeExpiration);
        }

        RestResponse<Map<String, Object>> restResponse = RestResponse.success(tokenData);

        response.setStatus(HttpStatus.OK.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), restResponse);
    }

    private static boolean isRememberMe(HttpServletRequest request) {
        String rememberMe = request.getParameter("rememberMe");
        if (rememberMe == null) {
            Object attr = request.getAttribute("rememberMe");
            if (attr instanceof Boolean) {
                return (Boolean) attr;
            }
            if (attr instanceof String) {
                return "true".equals(attr);
            }
            return false;
        }
        return "true".equals(rememberMe) || "on".equals(rememberMe);
    }
}
