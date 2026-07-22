package com.github.zeng.alt.security.rbac.serve.handler;

import com.github.zeng.alt.security.api.HttpResource;
import com.github.zeng.alt.security.rbac.serve.locator.ReactivePermissionLocator;
import com.github.zeng.alt.security.rbac.serve.manager.ReactiveResourceQueryManager;
import com.github.zeng.alt.security.rbac.serve.router.RouteTemplateManager;
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
 * 鉴权逻辑：</p>
 * <ol>
 *   <li>通过 {@link RouteTemplateManager#match(String)} 将实际路径归一化为路由模板</li>
 *   <li>通过 {@link ReactiveResourceQueryManager#queryPermissionForResource} 获取该路由所需的权限标识</li>
 *   <li>通过 {@link ReactivePermissionLocator#load(Mono)} 获取当前用户的权限集合</li>
 *   <li>判断用户权限是否包含所需权限</li>
 * </ol>
 */
@Slf4j
public class ReactiveHttpResourceHandler extends AbstractReactiveResourceHandler {

    private final ReactivePermissionLocator permissionLocator;
    private final RouteTemplateManager routeTemplateManager;

    public ReactiveHttpResourceHandler(ReactiveResourceQueryManager reactiveResourceQueryManager, RouteTemplateManager routeTemplateManager, ReactivePermissionLocator permissionLocator) {
        super(reactiveResourceQueryManager);
        this.permissionLocator = permissionLocator;
        this.routeTemplateManager = routeTemplateManager;
    }

    @Override
    public Mono<ServerWebExchangeMatcher.MatchResult> matcher(ServerWebExchange exchange) {
        return ServerWebExchangeMatcher.MatchResult.match();
    }

    @Override
    public Mono<Boolean> handler(Mono<Authentication> authentication, AuthorizationContext object) {
        String path = object.getExchange().getRequest().getURI().getPath();
        String method = object.getExchange().getRequest().getMethod().name();
        String template = this.routeTemplateManager.match(path);

        log.debug("Authorizing {} {} (template: {})", method, path, template);

        Mono<Set<String>> userPermissions = permissionLocator.load(authentication);
        return this.reactiveResourceQueryManager
                .queryPermissionForResource(create(template, method), authentication)
                .flatMap(requiredPermission -> userPermissions.map(permissions -> {
                    boolean granted = permissions.contains(requiredPermission);
                    if (granted) {
                        log.info("GRANT {} {} to user", method, path);
                    } else {
                        log.warn("DENY {} {} to user (required permission: {})", method, path, requiredPermission);
                    }
                    return granted;
                }));
    }

    private HttpResource create(String path, String method) {
        HttpResource httpResource = new HttpResource();
        httpResource.setUri(path);
        httpResource.setMethod(method);
        return httpResource;
    }
}
