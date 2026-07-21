package com.github.zeng.alt.security.rbac.serve.locator;


import com.github.zeng.alt.security.api.HttpResource;
import com.github.zeng.alt.security.api.Resource;
import com.github.zeng.alt.security.rbac.serve.repository.RbacResourceService;
import com.github.zeng.alt.tenant.api.TenantDetail;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2024年12月06日 21:09
 */
public class ReactiveHttpResourceLocator extends AbstractReactiveResourceLocator {

    private final RbacResourceService rbacResourceService;

    public ReactiveHttpResourceLocator(RbacResourceService rbacResourceService) {
        this.rbacResourceService = rbacResourceService;
    }

    protected List<Resource> list(Object o) {
        if (o == null) {
            return new ArrayList<>();
        }
        String tenantName = null;
        String username = "";
        List<String> authorities = new ArrayList<>();
        if (o instanceof TenantDetail tenantDetail) {
            tenantName = tenantDetail.getTenantName();
        }
        if (o instanceof UserDetails userDetails) {
            username = userDetails.getUsername();
            authorities = userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        }
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
        return permission == null ? "" : permission;
    }

    @Override
    protected void verifyInstance(Resource resource) {
        Assert.isInstanceOf(HttpResource.class, resource,"Only HttpResource is supported");
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
