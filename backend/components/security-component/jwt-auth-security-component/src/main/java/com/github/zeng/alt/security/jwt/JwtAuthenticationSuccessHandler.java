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

        RestResponse<Map<String, Object>> restResponse = RestResponse.success(tokenData);

        response.setStatus(HttpStatus.OK.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), restResponse);
    }
}
