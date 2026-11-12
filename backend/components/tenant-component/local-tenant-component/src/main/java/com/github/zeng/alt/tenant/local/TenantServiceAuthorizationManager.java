package com.github.zeng.alt.tenant.local;

import com.github.zeng.alt.security.api.AuthorizationManagerProvider;
import org.springframework.core.Ordered;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.function.Supplier;

/**
 * 服务身份授权管理器：只认“服务身份 × 元数据路径”，其余一律弃权。
 * <p>
 * 组合管理器是 first-grant-wins，本管理器返回 false 时自动落到后面的
 * 白名单 / 超管 / RBAC，不影响任何正常链路。
 *
 * @author zengJiaJun
 * @since 2026年10月02日
 * @version 1.0
 */
public class TenantServiceAuthorizationManager
        implements AuthorizationManager<RequestAuthorizationContext>,
        AuthorizationManagerProvider<RequestAuthorizationContext>, Ordered {

    private final TenantStoreProperties properties;

    public TenantServiceAuthorizationManager(TenantStoreProperties properties) {
        this.properties = properties;
    }

    @Override
    public AuthorizationManager<RequestAuthorizationContext> get() {
        return this;
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public AuthorizationDecision authorize(
            Supplier<? extends Authentication> authentication, RequestAuthorizationContext context) {
        Authentication auth = authentication.get();
        if (auth instanceof TenantServiceAuthentication && auth.isAuthenticated()
                && isMetadataPath(context.getRequest().getRequestURI(),
                context.getRequest().getContextPath())) {
            return new AuthorizationDecision(true);
        }
        return new AuthorizationDecision(false);
    }

    private boolean isMetadataPath(String uri, String contextPath) {
        String path = properties.getPath();
        if (path == null || path.isBlank()) {
            return false;
        }
        if (contextPath != null && !contextPath.isBlank() && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        return uri.equals(path) || uri.startsWith(path.endsWith("/") ? path : path + "/");
    }
}
