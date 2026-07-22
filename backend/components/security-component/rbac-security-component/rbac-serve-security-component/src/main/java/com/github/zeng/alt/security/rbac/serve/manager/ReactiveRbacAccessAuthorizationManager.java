package com.github.zeng.alt.security.rbac.serve.manager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import reactor.core.publisher.Mono;

/**
 * Reactive 环境 RBAC 授权管理器。
 *
 * <p>实现 {@link ReactiveAuthorizationManager}，通过 {@link ReactiveParseManager} 解析请求
 * 并分发到对应的 {@link com.github.zeng.alt.security.rbac.serve.handler.ReactiveResourceHandler} 完成鉴权。</p>
 */
@Slf4j
public final class ReactiveRbacAccessAuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext> {

    private final ReactiveParseManager reactiveParseManager;

    public ReactiveRbacAccessAuthorizationManager(ReactiveParseManager reactiveParseManager) {
        this.reactiveParseManager = reactiveParseManager;
    }

    @Override
    public Mono<AuthorizationDecision> check(Mono<Authentication> authentication, AuthorizationContext object) {
        String path = object.getExchange().getRequest().getURI().getPath();
        return reactiveParseManager
                .parse(object.getExchange())
                .flatMap(handler -> handler.handler(authentication, object))
                .doOnNext(granted -> {
                    if (granted) {
                        log.info("GRANT request {} to user", path);
                    } else {
                        log.warn("DENY request {} to user", path);
                    }
                })
                .map(AuthorizationDecision::new)
                .defaultIfEmpty(new AuthorizationDecision(false));
    }
}
