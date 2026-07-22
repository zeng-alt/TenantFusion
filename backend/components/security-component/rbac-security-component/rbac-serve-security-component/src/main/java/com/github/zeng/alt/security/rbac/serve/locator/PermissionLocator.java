package com.github.zeng.alt.security.rbac.serve.locator;

import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.rbac.serve.repository.RbacResourceService;
import com.github.zeng.alt.tenant.api.TenantDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Servlet 环境权限定位器。
 *
 * <p>{@link com.github.zeng.alt.security.rbac.serve.locator.ReactivePermissionLocator} 的同步版本。
 * 根据认证主体的角色信息，从 {@link RbacResourceService} 查询用户拥有的全部权限标识集合。
 * 优先使用 {@link SecurityUser#getCurrentRole()}（当前激活角色），否则使用全部角色。</p>
 */
@Slf4j
public class PermissionLocator {

    private final RbacResourceService rbacResourceService;

    public PermissionLocator(RbacResourceService rbacResourceService) {
        this.rbacResourceService = rbacResourceService;
    }

    /**
     * 根据认证主体查询其拥有的权限标识。
     *
     * @param principal 认证主体
     * @return 权限标识集合
     */
    public Set<String> findPermissions(Object principal) {
        if (principal == null) {
            log.trace("Principal is null, returning empty permissions");
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
        log.debug("Finding permissions for tenant '{}' with {} authorities", tenantName, authorities.size());
        return rbacResourceService.findPermission(authorities, tenantName);
    }

    private Object getAuthorizationPrincipal(Authentication authentication) {
        return authentication.getPrincipal();
    }

    private boolean isNotAnonymous(Authentication authentication) {
        return authentication.isAuthenticated();
    }

    /**
     * 加载当前认证用户的权限集合。
     *
     * @param authentication 当前认证信息
     * @return 用户拥有的权限标识集合
     */
    public Set<String> load(Authentication authentication) {
        if (!isNotAnonymous(authentication)) {
            return new HashSet<>();
        }
        return findPermissions(getAuthorizationPrincipal(authentication));
    }
}
