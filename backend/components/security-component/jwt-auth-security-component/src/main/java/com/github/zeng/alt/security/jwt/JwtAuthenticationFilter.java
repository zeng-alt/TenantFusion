package com.github.zeng.alt.security.jwt;

import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.core.properties.LoginProperties;
import com.github.zeng.alt.storage.StorageTemplate;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器，从请求头提取 Bearer token 并验证.
 * <p>
 * 验证流程：提取 token → 校验签名和过期 → 校验缓存中是否存在（未被登出）→
 * 重建 {@link SecurityUser} 并写入 SecurityContext。
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

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   StorageTemplate storageTemplate,
                                   String headerName,
                                   LoginProperties loginProperties) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.storageTemplate = storageTemplate;
        this.headerName = headerName;
        this.loginRequestMatcher = PathPatternRequestMatcher.withDefaults().matcher(loginProperties.getMethod(), loginProperties.getLoginPath());
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

        // 没有 Authorization 头，放行让后续过滤器处理
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        // 1. 校验 JWT 签名和过期时间
        if (!jwtTokenProvider.validateToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. 校验缓存中是否存在该 token（未被登出）
        String cacheKey = jwtTokenProvider.getCacheKey(token);
        if (cacheKey == null || Boolean.FALSE.equals(storageTemplate.hasKey(cacheKey))) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. 重建用户并设置 SecurityContext
        Claims claims = jwtTokenProvider.getClaims(token);
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
    }
}
