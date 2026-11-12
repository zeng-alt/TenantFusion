package com.github.zeng.alt.security.cookie;

import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.core.properties.LoginProperties;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
import java.util.Objects;

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
    private final UserDetailsService users;

    /**
     * 兼容旧构造签名；缺少权威账户服务时拒绝恢复会话。
     * @deprecated 使用提供 UserDetailsService 的构造器。
     */
    @Deprecated
    public CookieAuthenticationFilter(SessionManager sessions, String cookieName, LoginProperties loginProperties) {
        this(sessions, cookieName, loginProperties, username -> {
            throw new UsernameNotFoundException("Cookie authentication requires UserDetailsService");
        });
    }

    /** 使用当前租户的权威账户服务复核缓存会话。 */
    public CookieAuthenticationFilter(
            SessionManager sessions, String cookieName, LoginProperties loginProperties, UserDetailsService users
    ) {
        this.sessionManager = sessions;
        this.cookieName = cookieName;
        this.users = users;
        this.loginRequestMatcher = PathPatternRequestMatcher.withDefaults()
                .matcher(loginProperties.getMethod(), loginProperties.getLoginPath());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain
    ) throws ServletException, IOException {
        final String sessionId = extractCookie(request, cookieName);
        if (loginRequestMatcher.matches(request) || sessionId == null) {
            filterChain.doFilter(request, response);
            return;
        }
        final SecurityUser snapshot = sessionManager.getSession(sessionId);
        if (snapshot == null) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            TenantContextHolder.clear();
            TenantContextHolder.setTenantId(snapshot.getTenant());
            final Option<SecurityUser> user = loadUser(snapshot);
            if (user.isEmpty()) {
                sessionManager.removeSession(sessionId);
                filterChain.doFilter(request, response);
                return;
            }
            SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                    user.get(), null, user.get().getAuthorities()));
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
            TenantContextHolder.clear();
        }
    }

    private Option<SecurityUser> loadUser(SecurityUser snapshot) {
        return Option.of(snapshot).filter(this::isActive)
                .flatMap(cached -> Try.of(() -> Option.of(users.loadUserByUsername(cached.getUsername())))
                        .recover(UsernameNotFoundException.class, ignored -> Option.none()).get())
                .filter(SecurityUser.class::isInstance).map(SecurityUser.class::cast)
                .filter(this::isActive)
                .filter(user -> Objects.equals(snapshot.getId(), user.getId())
                        && Objects.equals(snapshot.getTenant(), user.getTenant()))
                .peek(user -> Option.of(snapshot.getCurrentRole()).flatMap(role -> Option.ofOptional(
                                user.getRoles().stream().filter(current -> current.getAuthority()
                                        .equalsIgnoreCase(role.getAuthority())).findFirst()))
                        .peek(user::setCurrentRole))
                .peek(SecurityUser::eraseCredentials);
    }

    private boolean isActive(SecurityUser user) {
        return user.isEnabled() && user.isAccountNonLocked()
                && user.isAccountNonExpired() && user.isCredentialsNonExpired();
    }

    private String extractCookie(HttpServletRequest request, String name) {
        return Option.of(request.getCookies()).flatMap(cookies -> Option.ofOptional(Arrays.stream(cookies)
                .filter(cookie -> name.equals(cookie.getName())).map(Cookie::getValue).findFirst())).getOrNull();
    }
}
