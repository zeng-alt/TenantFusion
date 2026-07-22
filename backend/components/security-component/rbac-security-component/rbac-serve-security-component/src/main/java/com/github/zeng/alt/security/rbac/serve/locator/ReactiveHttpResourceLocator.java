package com.github.zeng.alt.security.rbac.serve.locator;

import com.github.zeng.alt.security.api.HttpResource;
import com.github.zeng.alt.security.api.Resource;
import com.github.zeng.alt.security.rbac.serve.repository.RbacResourceService;
import com.github.zeng.alt.tenant.api.TenantDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * Reactive 环境 HTTP 资源定位器。
 *
 * <p>从 {@link RbacResourceService} 加载当前用户可访问的 HTTP 资源列表及权限标识。</p>
 */
@Slf4j
public class ReactiveHttpResourceLocator extends AbstractReactiveResourceLocator {

    private final RbacResourceService rbacResourceService;

    public ReactiveHttpResourceLocator(RbacResourceService rbacResourceService) {
        this.rbacResourceService = rbacResourceService;
    }

    @Override
    protected List<Resource> list(Object principal) {
        if (principal == null) {
            log.trace("Principal is null, returning empty resource list");
            return new ArrayList<>();
        }
        String tenantName = null;
        String username = "";
        List<String> authorities = new ArrayList<>();
        if (principal instanceof TenantDetail tenantDetail) {
            tenantName = tenantDetail.getTenantName();
        }
        if (principal instanceof UserDetails userDetails) {
            username = userDetails.getUsername();
            authorities = userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        }
        log.debug("Loading HTTP resources for user '{}' in tenant '{}' with {} authorities",
                username, tenantName, authorities.size());
        return rbacResourceService.findAllHttpResource(username, tenantName, authorities);
    }

    @Override
    protected String loadPermissionForResource(Resource resource, Object principal) {
        if (principal == null) {
            return "";
        }
        String tenantName = null;
        if (principal instanceof TenantDetail tenantDetail) {
            tenantName = tenantDetail.getTenantName();
        }
        String permission = rbacResourceService.findPermissionByResource(tenantName, resource.getKey());
        log.trace("Permission for resource [{}] in tenant '{}': {}", resource.getKey(), tenantName, permission);
        return permission == null ? "" : permission;
    }

    @Override
    protected void verifyInstance(Resource resource) {
        Assert.isInstanceOf(HttpResource.class, resource, "Only HttpResource is supported");
    }

    @Override
    public boolean supports(Class<?> resource) {
        return (HttpResource.class.isAssignableFrom(resource));
    }

    @Override
    public int getOrder() {
        return 30;
    }
}
