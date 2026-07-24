package com.github.zeng.alt.security.rbac.serve.handler;

import com.github.zeng.alt.security.rbac.serve.locator.ReactivePermissionLocator;
import com.github.zeng.alt.security.rbac.serve.repository.RbacResourceService;
import com.github.zeng.alt.security.rbac.serve.router.RouteTemplateManager;
import com.github.zeng.alt.tenant.api.TenantDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * Reactive 环境默认 HTTP 资源处理器。
 *
 * <p>作为 {@link ReactiveParseManager} 的最终 fallback，始终匹配。
 * 鉴权逻辑：根据当前请求的 method + path 获取所需的权限 code，
 * 与当前角色拥有的全部权限 code 集合比对，判断是否授权。</p>
 */
@Slf4j
public class ReactiveHttpResourceHandler implements ReactiveResourceHandler {

    private final ReactivePermissionLocator permissionLocator;
    private final RouteTemplateManager routeTemplateManager;
    private final RbacResourceService rbacResourceService;

    public ReactiveHttpResourceHandler(RouteTemplateManager routeTemplateManager, ReactivePermissionLocator permissionLocator, RbacResourceService rbacResourceService) {
        this.routeTemplateManager = routeTemplateManager;
        this.permissionLocator = permissionLocator;
        this.rbacResourceService = rbacResourceService;
    }

    @Override
    public Mono<ServerWebExchangeMatcher.MatchResult> matcher(ServerWebExchange exchange) {
        return ServerWebExchangeMatcher.MatchResult.match();
    }

    @Override
    public Mono<Boolean> handler(Mono<Authentication> authentication, AuthorizationContext object) {
        String path = object.getExchange().getRequest().getURI().getPath();
        String method = object.getExchange().getRequest().getMethod().name();
        String template = this.routeTemplateManager.match(method, path);

        log.debug("Authorizing {} {} (template: {})", method, path, template);

        return authentication.flatMap(auth ->
                permissionLocator.load(Mono.just(auth)).map(userPermissions -> {
                    String requiredPermission = rbacResourceService.findPermissionByMethodAndPath(
                            resolveTenant(auth), method, template);
                    boolean granted = requiredPermission != null && userPermissions.contains(requiredPermission);
                    if (granted) {
                        log.info("GRANT {} {} to user", method, path);
                    } else {
                        log.warn("DENY {} {} to user (required permission: {})", method, path, requiredPermission);
                    }
                    return granted;
                })
        );
    }

    private static String resolveTenant(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof TenantDetail tenantDetail) {
            return tenantDetail.getTenantName();
        }
        return "";
    }
}
