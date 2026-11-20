package com.github.zeng.alt.security.abac.server.security;

import com.github.zeng.alt.security.abac.AbacFailure;
import com.github.zeng.alt.security.abac.policy.AbacPolicyQueryService;
import com.github.zeng.alt.security.abac.server.config.AbacServerProperties;
import com.github.zeng.alt.security.abac.server.config.AbacServiceClientProperties;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import com.github.zeng.alt.tenant.api.TenantRouting;
import io.vavr.control.Either;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 独立服务身份入口；凭据校验后才建立租户路由，结束时还原原有安全及租户上下文。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor
public class AbacServiceAuthenticationFilter extends OncePerRequestFilter {
    private final AbacServerProperties properties;
    private final AuthenticationEntryPoint entryPoint;
    private final AccessDeniedHandler denied;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !AbacServiceAuthorizationManager.REQUEST.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getHeader(AbacPolicyQueryService.CLIENT_HEADER) == null
                && request.getHeader(AbacPolicyQueryService.KEY_HEADER) == null) {
            chain.doFilter(request, response);
            return;
        }
        final Either<AbacFailure, AbacServiceAuthentication> authenticated = authenticate(request);
        if (authenticated.isLeft()) {
            if ("SERVICE_UNAUTHENTICATED".equals(authenticated.getLeft().code())) {
                entryPoint.commence(request, response, new BadCredentialsException(authenticated.getLeft().message()));
                return;
            }
            denied.handle(request, response, new AccessDeniedException(authenticated.getLeft().message()));
            return;
        }
        final SecurityContext previous = SecurityContextHolder.getContext();
        final String tenant = TenantContextHolder.getTenantId();
        final String database = TenantContextHolder.getDatabase();
        final String schema = TenantContextHolder.getSchema();
        final TenantRouting routing = TenantContextHolder.getRouting();
        try {
            final SecurityContext current = SecurityContextHolder.createEmptyContext();
            current.setAuthentication(authenticated.get());
            SecurityContextHolder.setContext(current);
            TenantContextHolder.clear();
            TenantContextHolder.setTenantId(authenticated.get().getTenant());
            chain.doFilter(request, response);
        } finally {
            restore(previous, tenant, database, schema, routing);
        }
    }

    private Either<AbacFailure, AbacServiceAuthentication> authenticate(HttpServletRequest request) {
        final String client = request.getHeader(AbacPolicyQueryService.CLIENT_HEADER);
        final AbacServiceClientProperties account = StringUtils.hasText(client) ? properties.getClients().get(client) : null;
        final String key = request.getHeader(AbacPolicyQueryService.KEY_HEADER);
        if (account == null || !StringUtils.hasText(account.getKey()) || !StringUtils.hasText(key)
                || !MessageDigest.isEqual(account.getKey().getBytes(StandardCharsets.UTF_8), key.getBytes(StandardCharsets.UTF_8))) {
            return Either.left(new AbacFailure("SERVICE_UNAUTHENTICATED", "内部服务身份凭据无效"));
        }
        final String tenant = request.getHeader(AbacPolicyQueryService.TENANT_HEADER);
        if (!StringUtils.hasText(tenant) || !tenant.equals(request.getParameter("tenant"))
                || !account.getTenants().contains(tenant)) {
            return Either.left(new AbacFailure("SERVICE_TENANT_DENIED", "服务身份无权读取请求租户的策略"));
        }
        return Either.right(new AbacServiceAuthentication(client, tenant));
    }

    private void restore(SecurityContext context, String tenant, String database, String schema, TenantRouting routing) {
        SecurityContextHolder.setContext(context);
        TenantContextHolder.clear();
        TenantContextHolder.setTenantId(tenant);
        TenantContextHolder.setDatabase(database);
        TenantContextHolder.setSchema(schema);
        TenantContextHolder.setRouting(routing);
    }
}
