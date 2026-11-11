package com.github.zeng.alt.security.core.web;

import com.github.zeng.alt.security.api.AuthorizationConstraint;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 只要有一个为true就返回AuthorizationDecision(true)
 * @author zengJiaJun
 * @version 1.0
 * @since 2024年11月27日 21:18
 */
public class CompositeAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final List<AuthorizationManager<RequestAuthorizationContext>> authorizationManagers;
    private final List<AuthorizationConstraint<RequestAuthorizationContext>> constraints;

    /**
     * 保留仅执行 OR 授权的构造方式。
     */
    public CompositeAuthorizationManager(List<AuthorizationManager<RequestAuthorizationContext>> authorizationManagers) {
        this(authorizationManagers, List.of());
    }

    /**
     * 按列表顺序执行 OR 授权，通过后顺序执行全部 AND 约束；任一阶段均短路。
     */
    public CompositeAuthorizationManager(
            List<AuthorizationManager<RequestAuthorizationContext>> authorizationManagers,
            List<AuthorizationConstraint<RequestAuthorizationContext>> constraints
    ) {
        this.authorizationManagers = authorizationManagers;
        this.constraints = constraints;
    }

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext object) {
        return authorizationManagers.stream()
                .map(manager -> manager.check(authentication, object))
                .filter(Objects::nonNull)
                .filter(AuthorizationDecision::isGranted)
                .findFirst()
                .map(granted -> constraints.stream()
                        .map(constraint -> constraint.check(authentication, object))
                        .allMatch(decision -> decision != null && decision.isGranted())
                        ? granted : new AuthorizationDecision(false))
                .orElseGet(() -> new AuthorizationDecision(false));
    }

    @Override
    public AuthorizationResult authorize(Supplier<Authentication> authentication, RequestAuthorizationContext object) {
        return authorizationManagers.stream()
                .map(manager -> manager.authorize(authentication, object))
                .filter(Objects::nonNull)
                .filter(AuthorizationResult::isGranted)
                .findFirst()
                .map(granted -> constraints.stream()
                        .map(constraint -> constraint.authorize(authentication, object))
                        .allMatch(decision -> decision != null && decision.isGranted())
                        ? granted : new AuthorizationDecision(false))
                .orElseGet(() -> new AuthorizationDecision(false));
    }
}
