package com.github.zeng.alt.admin.interfaces.rest;

import com.github.zeng.alt.api.rest.RestResponse;
import com.github.zeng.alt.security.api.UserContextHolder;
import com.github.zeng.alt.security.core.TenantSwitchTemplate;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import com.github.zeng.alt.tenant.api.TenantMetadata;
import io.vavr.control.Option;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 内置管理员选择操作租户的入口。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/auth")
public class TenantSwitchController {
    private final TenantSwitchTemplate tenantSwitch;

    /** 返回可切换的启用租户。 */
    @GetMapping("/tenants")
    public RestResponse<List<Map<String, String>>> list() {
        final List<TenantMetadata> tenants = tenantSwitch.list(UserContextHolder.getSecurityUser())
                .getOrElseThrow(AccessDeniedException::new);
        return RestResponse.success(tenants.stream()
                .map(tenant -> Map.of("id", tenant.tenantId(),
                        "name", Option.of(tenant.tenantName()).getOrElse(tenant.tenantId()))).toList());
    }

    /** 返回经过认证过滤器校验的操作租户。 */
    @GetMapping("/tenant")
    public RestResponse<String> getTenant() {
        if (!tenantSwitch.canSwitch(UserContextHolder.getSecurityUser())) {
            throw new AccessDeniedException("无权切换租户");
        }
        return RestResponse.success(TenantContextHolder.getTenantId());
    }
}
