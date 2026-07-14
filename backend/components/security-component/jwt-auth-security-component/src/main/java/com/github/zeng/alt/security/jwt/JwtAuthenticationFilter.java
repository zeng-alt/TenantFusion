package com.github.zeng.alt.security.jwt;

import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.core.properties.LoginProperties;
import com.github.zeng.alt.storage.StorageTemplate;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * JWT 认证过滤器，从请求头提取 Bearer token 并验证.
 * <p>
 * 验证流程：提取 token → 校验签名和过期 → 校验缓存中是否存在（未被登出）→
 * 重建 {@link SecurityUser} 并写入 SecurityContext。
 * <p>
 * 支持记住我功能：accessToken 过期时，检查 {@code X-Refresh-Token} 请求头，
 * 若 refreshToken 有效则自动签发新 accessToken 并通过 {@code X-New-Access-Token} 响应头下发，
 * 前端读取后更新本地存储，实现无感续期。
 *
 * @author zengJiaJun
 * @version 1.0
 * @since 2024年10月07日
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final StorageTemplate storageTemplate;
    private final String headerName;
    private final RequestMatcher loginRequestMatcher;
    private final String refreshTokenHeader;
    private final String newAccessTokenHeader;
    private final long rememberMeExpiration;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   StorageTemplate storageTemplate,
                                   String headerName,
                                   LoginProperties loginProperties,
                                   String refreshTokenHeader,
                                   String newAccessTokenHeader,
                                   long rememberMeExpiration) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.storageTemplate = storageTemplate;
        this.headerName = headerName;
        this.loginRequestMatcher = PathPatternRequestMatcher.withDefaults().matcher(loginProperties.getMethod(), loginProperties.getLoginPath());
        this.refreshTokenHeader = refreshTokenHeader;
        this.newAccessTokenHeader = newAccessTokenHeader;
        this.rememberMeExpiration = rememberMeExpiration;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 登录路径不需要 JWT 认证
        if (loginRequestMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(headerName);
        String refreshToken = request.getHeader(refreshTokenHeader);

        // 没有 Authorization 头，放行让后续过滤器处理
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        // 1. 校验 JWT 签名和过期时间
        JwtTokenProvider.TokenValidationResult result = jwtTokenProvider.validateTokenWithResult(token);

        if (result == JwtTokenProvider.TokenValidationResult.VALID) {
            // 2. 校验缓存中是否存在该 token（未被登出）
            String cacheKey = jwtTokenProvider.getCacheKey(token);
            if (cacheKey == null || Boolean.FALSE.equals(storageTemplate.hasKey(cacheKey))) {
                filterChain.doFilter(request, response);
                return;
            }

            // 3. 重建用户并设置 SecurityContext
            Jwt claims = jwtTokenProvider.getClaims(token);
            if (claims == null) {
                filterChain.doFilter(request, response);
                return;
            }

            SecurityUser securityUser = jwtTokenProvider.getUserFromClaims(claims);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(authentication);

            try {
                filterChain.doFilter(request, response);
            } finally {
                SecurityContextHolder.clearContext();
            }
            return;
        }

        // ===== accessToken 过期，尝试用 refreshToken 无感续期 =====
        if (result == JwtTokenProvider.TokenValidationResult.EXPIRED
                && StringUtils.hasText(refreshToken)
                && jwtTokenProvider.validateToken(refreshToken)) {

            Jwt refreshClaims = jwtTokenProvider.getClaims(refreshToken);
            if (refreshClaims == null || !jwtTokenProvider.isRefreshToken(refreshClaims)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 校验 refreshToken 缓存（未被登出）
            String refreshCacheKey = jwtTokenProvider.getRefreshCacheKey(refreshToken);
            if (refreshCacheKey == null || Boolean.FALSE.equals(storageTemplate.hasKey(refreshCacheKey))) {
                filterChain.doFilter(request, response);
                return;
            }

            // 从 refreshToken claims 重建用户
            SecurityUser securityUser = jwtTokenProvider.getUserFromClaims(refreshClaims);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());

            // 签发新 accessToken
            String newAccessToken = jwtTokenProvider.createToken(securityUser);

            // 新 accessToken 存入缓存
            String newAccessCacheKey = jwtTokenProvider.getCacheKey(newAccessToken);
            if (newAccessCacheKey != null) {
                storageTemplate.opsForString().set(
                        newAccessCacheKey,
                        securityUser.getUsername(),
                        Duration.ofSeconds(jwtTokenProvider.getExpirationSeconds())
                );
            }

            // 删除旧 accessToken 缓存
            String oldAccessCacheKey = jwtTokenProvider.getCacheKeyFromExpiredToken(token);
            if (oldAccessCacheKey != null) {
                storageTemplate.delete(oldAccessCacheKey);
            }

            // 设置响应头，前端读取后更新本地 accessToken
            response.setHeader(newAccessTokenHeader, newAccessToken);

            SecurityContextHolder.getContext().setAuthentication(authentication);

            try {
                filterChain.doFilter(request, response);
            } finally {
                SecurityContextHolder.clearContext();
            }
            return;
        }

        // accessToken 无效或无有效 refreshToken，放行（后续 401）
        filterChain.doFilter(request, response);
    }
}
