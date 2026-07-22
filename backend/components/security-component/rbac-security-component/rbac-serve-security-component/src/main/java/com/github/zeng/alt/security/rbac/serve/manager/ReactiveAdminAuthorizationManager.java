package com.github.zeng.alt.security.rbac.serve.manager;

import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.core.properties.SecurityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * Reactive 超级管理员授权管理器。
 *
 * <p>如果认证用户名为 {@code superAdmin}，则直接放行（绕过 RBAC 权限检查）。
 * 适用于运维/管理端接口的完全开放。</p>
 */
@Slf4j
@RequiredArgsConstructor
public class ReactiveAdminAuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext> {

    private final SecurityProperties securityProperties;

    @Override
    public Mono<AuthorizationDecision> check(Mono<Authentication> authentication, AuthorizationContext object) {
        return authentication
                .filter(auth -> auth.isAuthenticated() && isAdmin(auth))
                .map(this::getAuthorizationDecision)
                .doOnNext(decision -> {
                    if (decision.isGranted()) {
                        log.debug("Super admin bypass for user '{}'", securityProperties.getAdmin());
                    }
                })
                .defaultIfEmpty(new AuthorizationDecision(false));
    }

    private AuthorizationDecision getAuthorizationDecision(Authentication authentication) {
        return new AuthorizationDecision(authentication.isAuthenticated());
    }

    private boolean isAdmin(Authentication authentication) {

        Object principal = authentication.getPrincipal();
        if (principal instanceof SecurityUser securityUser) {
            return securityProperties.getAdmin().getId().equalsIgnoreCase(securityUser.getId())
                    || securityProperties.getAdmin().getCode().equalsIgnoreCase(Optional.ofNullable(securityUser.getCurrentRole()).map(GrantedAuthority::getAuthority).orElse(null));
        }
        return false;
    }
}
