package com.github.zeng.alt.security.core.reactive;

import com.github.zeng.alt.security.api.ReactiveAuthorizationConstraint;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author zengJiaJun
 * @version 1.0
 * @since 2024年11月27日 21:25
 */
public class CompositeReactiveAuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext> {

    private final List<ReactiveAuthorizationManager<AuthorizationContext>> managers;
    private final List<ReactiveAuthorizationConstraint<AuthorizationContext>> constraints;

    /**
     * 保留仅执行 OR 授权的构造方式。
     */
    public CompositeReactiveAuthorizationManager(List<ReactiveAuthorizationManager<AuthorizationContext>> managers) {
        this(managers, List.of());
    }

    /**
     * 按列表顺序订阅 OR 授权，通过后顺序订阅全部 AND 约束；任一阶段均短路。
     */
    public CompositeReactiveAuthorizationManager(
            List<ReactiveAuthorizationManager<AuthorizationContext>> managers,
            List<ReactiveAuthorizationConstraint<AuthorizationContext>> constraints
    ) {
        this.managers = managers;
        this.constraints = constraints;
    }

    @Override
    public Mono<AuthorizationDecision> check(Mono<Authentication> authentication, AuthorizationContext context) {
        return Flux.fromIterable(managers)
                .concatMap(manager -> {
                    final Mono<AuthorizationDecision> decision = manager.check(authentication, context);
                    return decision == null ? Mono.empty() : decision;
                })
                .filter(AuthorizationDecision::isGranted)
                .next()
                .flatMap(granted -> Flux.fromIterable(constraints)
                        .concatMap(constraint -> {
                            final Mono<AuthorizationDecision> decision = constraint.check(authentication, context);
                            return decision == null ? Mono.just(new AuthorizationDecision(false))
                                    : decision.defaultIfEmpty(new AuthorizationDecision(false));
                        })
                        .all(AuthorizationDecision::isGranted)
                        .map(AuthorizationDecision::new))
                .defaultIfEmpty(new AuthorizationDecision(false));
    }
}
