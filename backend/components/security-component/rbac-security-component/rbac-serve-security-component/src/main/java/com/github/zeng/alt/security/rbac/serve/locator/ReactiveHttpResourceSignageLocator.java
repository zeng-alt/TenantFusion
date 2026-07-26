package com.github.zeng.alt.security.rbac.serve.locator;

import com.github.zeng.alt.security.api.Resource;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.rbac.serve.repository.RbacResourceService;
import com.github.zeng.alt.tenant.api.TenantDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

/**
 * @author zengJiaJun
 * @since 2026年07月25日
 * @version 1.0
 */
@RequiredArgsConstructor
public class ReactiveHttpResourceSignageLocator implements ReactiveResourceSignageLocator {

    private final RbacResourceService resourceService;

    @Override
    public Mono<String> load(Resource resource, Authentication authentication) {
        String tenant = null;
        if (authentication instanceof TenantDetail tenantDetail) {
            tenant = tenantDetail.getTenantName();
        }
        return Mono.just(resourceService.findPermission(tenant, resource));
    }
}
