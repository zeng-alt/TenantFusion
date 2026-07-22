package com.github.zeng.alt.security.rbac.serve.manager;

import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.core.properties.SecurityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Servlet 超级管理员授权管理器。
 *
 * <p>{@link com.github.zeng.alt.security.rbac.serve.manager.ReactiveAdminAuthorizationManager} 的同步版本。
 * 如果认证用户名为 {@code superAdmin}，则直接放行（绕过 RBAC 权限检查）。
 * 适用于运维/管理端接口的完全开放。</p>
 */
@Slf4j
@RequiredArgsConstructor
public class AdminAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final SecurityProperties securityProperties;

    @Override
    public AuthorizationDecision check(Supplier<Authentication> supplier, RequestAuthorizationContext object) {
        Authentication authentication = supplier.get();
        if (authentication == null) {
            return new AuthorizationDecision(false);
        }
        if (authentication.isAuthenticated() && isAdmin(authentication)) {
            log.debug("Super admin bypass for user '{}'", securityProperties.getAdmin());
            return new AuthorizationDecision(authentication.isAuthenticated());
        }
        return new AuthorizationDecision(false);
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
