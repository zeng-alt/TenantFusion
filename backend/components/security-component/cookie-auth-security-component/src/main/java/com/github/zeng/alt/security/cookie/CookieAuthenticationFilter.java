package com.github.zeng.alt.security.cookie;

import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.core.properties.LoginProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

/**
 * Cookie 认证过滤器，从请求 Cookie 中提取 sessionId 并验证.
 * <p>
 * 验证流程：读取 Cookie → 按 sessionId 查询缓存 → 重建 {@link SecurityUser} → 写入 SecurityContext。
 * 登录路径的请求直接放行。
 *
 * @author zengJiaJun
 * @version 1.0
 * @since 2024年10月07日
 */
public class CookieAuthenticationFilter extends OncePerRequestFilter {

    private final SessionManager sessionManager;
    private final String cookieName;
    private final RequestMatcher loginRequestMatcher;

    public CookieAuthenticationFilter(SessionManager sessionManager, String cookieName, LoginProperties loginProperties) {
        this.sessionManager = sessionManager;
        this.cookieName = cookieName;
        this.loginRequestMatcher = PathPatternRequestMatcher.withDefaults().matcher(loginProperties.getMethod(), loginProperties.getLoginPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 登录路径不需要 Cookie 认证
        if (loginRequestMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 读取 Session Cookie
        String sessionId = extractCookie(request, cookieName);

        // 没有 Session Cookie，放行让后续过滤器处理
        if (sessionId == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 从缓存中查询会话
        SecurityUser securityUser = sessionManager.getSession(sessionId);

        // 会话不存在或已过期
        if (securityUser == null) {
            filterChain.doFilter(request, response);
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private String extractCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
