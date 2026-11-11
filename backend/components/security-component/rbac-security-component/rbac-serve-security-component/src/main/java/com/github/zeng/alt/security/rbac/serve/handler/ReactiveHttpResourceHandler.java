package com.github.zeng.alt.security.rbac.serve.handler;

import com.github.zeng.alt.security.api.HttpResource;
import com.github.zeng.alt.security.rbac.serve.config.UnmatchedUrlPolicy;
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

import java.util.Set;

/**
 * Reactive 环境默认 HTTP 资源处理器。
 *
 * <p>作为 {@link ReactiveParseManager} 的最终 fallback，始终匹配。
 * 先按 method + path 取所需权限 code：无权限码（URL 无匹配）时按 {@link UnmatchedUrlPolicy} 放行或拒绝；
 * 有权限码时无论何种模式都必须用户持有该码才授权。行为与 Servlet 版一致。</p>
 */
@Slf4j
public class ReactiveHttpResourceHandler implements ReactiveResourceHandler {

    private final ReactivePermissionLocator permissionLocator;
    private final RouteTemplateManager routeTemplateManager;
    private final ReactiveResourceSignageLocator reactiveResourceSignageLocator;
    private final UnmatchedUrlPolicy unmatchedUrlPolicy;

    public ReactiveHttpResourceHandler(RouteTemplateManager routeTemplateManager, ReactivePermissionLocator permissionLocator, ReactiveResourceSignageLocator reactiveResourceSignageLocator, UnmatchedUrlPolicy unmatchedUrlPolicy) {
        this.routeTemplateManager = routeTemplateManager;
        this.permissionLocator = permissionLocator;
        this.reactiveResourceSignageLocator = reactiveResourceSignageLocator;
        this.unmatchedUrlPolicy = unmatchedUrlPolicy;
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
                reactiveResourceSignageLocator.load(HttpResource.of(template, method), auth)
                        .defaultIfEmpty("")
                        .flatMap(required -> {
                            if (!StringUtils.hasText(required)) {
                                return Mono.just(unmatchedUrlPolicy == UnmatchedUrlPolicy.PERMIT);
                            }
                            return permissionLocator.load(auth).defaultIfEmpty(Set.of())
                                    .map(permissions -> permissions.contains(required));
                        }));
    }
}