package com.github.zeng.alt.security.rbac.serve.handler;

import com.github.zeng.alt.security.api.HttpResource;
import com.github.zeng.alt.security.rbac.serve.config.UnmatchedUrlPolicy;
import com.github.zeng.alt.security.rbac.serve.locator.PermissionLocator;
import com.github.zeng.alt.security.rbac.serve.locator.ResourceSignageLocator;
import com.github.zeng.alt.security.rbac.serve.router.RouteTemplateManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.util.StringUtils;

/**
 * Servlet 环境默认 HTTP 资源处理器。
 *
 * <p>作为 {@link ParseManager} 的最终 fallback，始终匹配（{@code matcher()} 返回 {@code true}）。
 * 先按 method + path 取所需权限 code：无权限码（URL 无匹配）时按 {@link UnmatchedUrlPolicy} 放行或拒绝；
 * 有权限码时无论何种模式都必须用户持有该码才授权。</p>
 */
@Slf4j
public class HttpResourceHandler implements ResourceHandler {

    private final RouteTemplateManager routeTemplateManager;
    private final PermissionLocator permissionLocator;
    private final ResourceSignageLocator resourceSignageLocator;
    private final UnmatchedUrlPolicy unmatchedUrlPolicy;

    public HttpResourceHandler(RouteTemplateManager routeTemplateManager, PermissionLocator permissionLocator, ResourceSignageLocator resourceSignageLocator, UnmatchedUrlPolicy unmatchedUrlPolicy) {
        this.routeTemplateManager = routeTemplateManager;
        this.permissionLocator = permissionLocator;
        this.resourceSignageLocator = resourceSignageLocator;
        this.unmatchedUrlPolicy = unmatchedUrlPolicy;
    }

    @Override
    public boolean matcher(HttpServletRequest request) {
        return true;
    }

    @Override
    public Boolean handler(Authentication authentication, RequestAuthorizationContext object) {
        String uri = object.getRequest().getRequestURI();
        String method = object.getRequest().getMethod();
        String template = this.routeTemplateManager.match(method, uri);
        String requiredPermission = resourceSignageLocator.load(HttpResource.of(template, method), authentication);
        if (!StringUtils.hasText(requiredPermission)) {
            boolean granted = unmatchedUrlPolicy == UnmatchedUrlPolicy.PERMIT;
            log.debug("{} {} {} (unmatched, policy={})", granted ? "GRANT" : "DENY", method, uri, unmatchedUrlPolicy);
            return granted;
        }
        boolean granted = permissionLocator.load(authentication).contains(requiredPermission);
        log.debug("{} {} {} to user '{}'", granted ? "GRANT" : "DENY", method, uri, authentication.getName());
        return granted;
    }
}