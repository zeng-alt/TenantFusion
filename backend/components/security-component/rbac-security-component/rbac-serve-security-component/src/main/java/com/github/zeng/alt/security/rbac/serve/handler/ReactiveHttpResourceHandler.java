package com.github.zeng.alt.security.rbac.serve.handler;

import com.github.zeng.alt.security.api.HttpResource;
import com.github.zeng.alt.security.rbac.serve.locator.ReactivePermissionLocator;
import com.github.zeng.alt.security.rbac.serve.locator.ReactiveResourceSignageLocator;
import com.github.zeng.alt.security.rbac.serve.router.RouteTemplateManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

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
    private final ReactiveResourceSignageLocator reactiveResourceSignageLocator;

    public ReactiveHttpResourceHandler(RouteTemplateManager routeTemplateManager, ReactivePermissionLocator permissionLocator, ReactiveResourceSignageLocator reactiveResourceSignageLocator) {
        this.routeTemplateManager = routeTemplateManager;
        this.permissionLocator = permissionLocator;
        this.reactiveResourceSignageLocator = reactiveResourceSignageLocator;
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
                Mono.zip(
                        reactiveResourceSignageLocator.load(
                                HttpResource.of(template, method),
                                auth
                        ),
                        permissionLocator.load(auth)
                )
        ).map(this::checkPermission);
    }
    private boolean checkPermission(Tuple2<String, Set<String>> tuple) {
        String requiredPermission = tuple.getT1();
        Set<String> permissions = tuple.getT2();

        return StringUtils.hasText(requiredPermission)
                && permissions.contains(requiredPermission);
    }
}
