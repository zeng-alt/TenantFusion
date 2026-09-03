package com.github.zeng.alt.tenant.core;

import com.github.zeng.alt.tenant.api.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 租户上下文的兜底来源与清理。
 * <p>
 * 有意排在 Spring Security 过滤器链<b>之前</b>，职责只有两件：
 * <ol>
 *   <li>请求头兜底：供服务间调用等拿不到 JWT 的场景指定租户，随后 JWT 过滤器解析出的租户会覆盖它</li>
 *   <li><b>请求结束清理 ThreadLocal</b>：{@code JwtAuthenticationFilter} 只写不清，
 *       Web 容器线程复用下会让下一个请求继承上一个租户</li>
 * </ol>
 * 路由解析<b>不</b>在这里做——那时 JWT 还没解析出租户，会缓存到错的路由。
 * 解析改由 {@link TenantContextTracker} 在首次访问数据库时惰性完成。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
public class TenantResolveFilter extends OncePerRequestFilter {

    /** 早于 Spring Security 默认的 -100 */
    public static final int ORDER = -200;

    private final TenantProperties properties;

    public TenantResolveFilter(TenantProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            String header = properties.getTenantHeader();
            if (StringUtils.hasText(header)) {
                String fromHeader = request.getHeader(header);
                if (StringUtils.hasText(fromHeader)) {
                    TenantContextHolder.setTenantId(fromHeader.trim());
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }
}
