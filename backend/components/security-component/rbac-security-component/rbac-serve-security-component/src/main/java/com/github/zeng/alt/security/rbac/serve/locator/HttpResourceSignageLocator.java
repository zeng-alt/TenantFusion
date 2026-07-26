package com.github.zeng.alt.security.rbac.serve.locator;

import com.github.zeng.alt.security.api.Resource;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.rbac.serve.repository.RbacResourceService;
import com.github.zeng.alt.tenant.api.TenantDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;

/**
 * @author zengJiaJun
 * @since 2026年07月25日
 * @version 1.0
 */
@RequiredArgsConstructor
public class HttpResourceSignageLocator implements ResourceSignageLocator {

    private final RbacResourceService resourceService;

    @Override
    public String load(Resource resource, Authentication authentication) {
        String tenant = null;
        if (authentication.getPrincipal() instanceof TenantDetail tenantDetail) {
            tenant = tenantDetail.getTenantName();
        }
        return resourceService.findPermission(tenant, resource);
    }
}
