package com.github.zeng.alt.tenant.local;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

/**
 * 服务密钥认证过滤器：校验 {@code X-Tenant-Service-Key}。
 * <p>
 * 命中（路径为元数据端点 + 密钥恒定时间比对通过）时放入
 * {@link TenantServiceAuthentication}，后续由 Spring Security 链里的
 * {@code TenantServiceAuthorizationManager} 放行；授权仍走统一链路，
 * 本过滤器不直接决定放行。
 * <p>
 * 未配置 {@code alt.tenant.store.service-key} 时本过滤器直接透传，
 * 该通道等于不存在。
 *
 * @author zengJiaJun
 * @since 2026年10月02日
 * @version 1.0
 */
public class TenantServiceKeyFilter extends OncePerRequestFilter {

    /** 服务密钥头：与用户 JWT 的 Authorization 头分离，避免互相干扰 */
    public static final String SERVICE_KEY_HEADER = "X-Tenant-Service-Key";

    private final TenantStoreProperties properties;

    public TenantServiceKeyFilter(TenantStoreProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            authenticate(request);
        } catch (RuntimeException e) {
            // 认证是“尽力而为”的：失败就当没带 key，走正常 JWT 链路，不在这里直接 401
            SecurityContextHolder.clearContext();
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request) {
        String configured = properties.getServiceKey();
        if (!StringUtils.hasText(configured)) {
            return;
        }
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }
        if (!isMetadataPath(request)) {
            return;
        }
        String presented = request.getHeader(SERVICE_KEY_HEADER);
        if (!StringUtils.hasText(presented)) {
            return;
        }
        if (!constantTimeEquals(configured.trim(), presented.trim())) {
            return;
        }
        SecurityContextHolder.getContext()
                .setAuthentication(new TenantServiceAuthentication());
    }

    private boolean isMetadataPath(HttpServletRequest request) {
        String path = properties.getPath();
        if (!StringUtils.hasText(path)) {
            return false;
        }
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.hasText(contextPath) && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        return uri.equals(path) || uri.startsWith(path.endsWith("/") ? path : path + "/");
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}
