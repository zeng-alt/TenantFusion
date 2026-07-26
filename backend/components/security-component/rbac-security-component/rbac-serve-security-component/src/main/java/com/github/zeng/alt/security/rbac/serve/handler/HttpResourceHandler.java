package com.github.zeng.alt.security.rbac.serve.handler;

import com.github.zeng.alt.security.api.HttpResource;
import com.github.zeng.alt.security.rbac.serve.locator.PermissionLocator;
import com.github.zeng.alt.security.rbac.serve.locator.ResourceSignageLocator;
import com.github.zeng.alt.security.rbac.serve.router.RouteTemplateManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Set;

/**
 * Servlet 环境默认 HTTP 资源处理器。
 *
 * <p>作为 {@link ParseManager} 的最终 fallback，始终匹配（{@code matcher()} 返回 {@code true}）。
 * 鉴权逻辑：根据当前请求的 method + path 获取所需的权限 code，
 * 与当前角色拥有的全部权限 code 集合比对，判断是否授权。</p>
 */
@Slf4j
public class HttpResourceHandler implements ResourceHandler {

    private final RouteTemplateManager routeTemplateManager;
    private final PermissionLocator permissionLocator;
    private final ResourceSignageLocator resourceSignageLocator;

    public HttpResourceHandler(RouteTemplateManager routeTemplateManager, PermissionLocator permissionLocator, ResourceSignageLocator resourceSignageLocator) {
        this.routeTemplateManager = routeTemplateManager;
        this.permissionLocator = permissionLocator;
        this.resourceSignageLocator = resourceSignageLocator;
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
        Set<String> userPermissions = permissionLocator.load(authentication);
        if (CollectionUtils.isEmpty(userPermissions)) {
            return false;
        }
        String requiredPermission = resourceSignageLocator.load(HttpResource.of(template, method), authentication);
        boolean granted = StringUtils.hasText(requiredPermission) && userPermissions.contains(requiredPermission);
        log.debug("{} {} {} to user '{}'", granted ? "GRANT" : "DENY", method, uri, authentication.getName());
        return granted;
    }
}
