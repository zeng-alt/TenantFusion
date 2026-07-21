package com.github.zeng.alt.security.rbac.serve.locator;


import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.rbac.serve.repository.RbacResourceService;
import com.github.zeng.alt.tenant.api.TenantDetail;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2025年04月07日 15:31
 */
public class ReactivePermissionLocator {

    private final RbacResourceService rbacResourceService;

    public ReactivePermissionLocator(RbacResourceService rbacResourceService) {
        this.rbacResourceService = rbacResourceService;
    }

    public Set<String> findPermissions(Object principal) {
        if (principal == null) {
            return new HashSet<>();
        }
        String tenantName = "";
        List<String> authorities = new ArrayList<>();
        if (principal instanceof TenantDetail tenantDetail) {
            tenantName = tenantDetail.getTenantName();
        }
        if (principal instanceof SecurityUser securityUser) {
            if (securityUser.getCurrentRole() != null) {
                authorities = List.of(securityUser.getCurrentRole().getAuthority());
            } else {
                authorities = securityUser.getRoles().stream().map(GrantedAuthority::getAuthority).toList();
            }
        } else if (principal instanceof UserDetails userDetails) {
            authorities = userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        }
        return rbacResourceService.findPermission(authorities, tenantName);
    }

    private Object getAuthorizationPrincipal(Authentication authentication) {
        return authentication.getPrincipal();
    }

    private boolean isNotAnonymous(Authentication authentication) {
        return authentication.isAuthenticated();
    }

    public Mono<Set<String>> load(Mono<Authentication> authentication) {
        return authentication
                .filter(this::isNotAnonymous)
                .map(this::getAuthorizationPrincipal)
                .map(this::findPermissions)
                .switchIfEmpty(Mono.empty());
    }
}
