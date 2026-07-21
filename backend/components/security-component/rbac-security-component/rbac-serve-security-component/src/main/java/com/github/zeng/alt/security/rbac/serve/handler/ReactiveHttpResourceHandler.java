package com.github.zeng.alt.security.rbac.serve.handler;


import com.github.zeng.alt.security.api.HttpResource;
import com.github.zeng.alt.security.rbac.serve.locator.ReactivePermissionLocator;
import com.github.zeng.alt.security.rbac.serve.manager.ReactiveResourceQueryManager;
import com.github.zeng.alt.security.rbac.serve.router.RouteTemplateManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2024年12月05日 21:12
 */
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
//        object.getExchange().getRequest().getPath()
        String path = object.getExchange().getRequest().getURI().getPath();
        String match = this.routeTemplateManager.match(path);
        Mono<Set<String>> load = permissionLocator.load(authentication);
        return this.reactiveResourceQueryManager
                .queryPermissionForResource(create(match, object.getExchange().getRequest().getMethod().name()), authentication)
                .flatMap(p -> load.map(permissions -> {
                    return permissions.contains(p);
                }));
//        return load.map(permissions -> permissions.contains(match));

//        return reactiveResourceQueryManager
//                .query(new HttpResource(), authentication)
//                .flatMapIterable(resources -> resources)
//                .flatMap(resource -> resource.compareTo(object.getExchange()).map(ServerWebExchangeMatcher.MatchResult::isMatch))
//                .any(Boolean::booleanValue)
//                .switchIfEmpty(Mono.just(false));
    }

    private HttpResource create(String path, String method) {
        HttpResource httpResource = new HttpResource();
        httpResource.setUri(path);
        httpResource.setMethod(method);
        return httpResource;
    }
}
