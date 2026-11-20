package com.github.zeng.alt.security.abac.client.remote;

import com.github.zeng.alt.security.abac.policy.AbacPolicyQueryService;
import com.github.zeng.alt.security.abac.policy.AbacPolicySnapshotDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 按服务名定位集群策略服务，不绑定固定 URL，也不传入用户 Authorization。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@FeignClient(name = "${security.abac.client.service-id:admin-application}", contextId = "abacPolicies",
        configuration = AbacFeignConfiguration.class)
public interface AbacPolicyFeignClient {
    @GetMapping(AbacPolicyQueryService.PATH)
    AbacPolicySnapshotDto get(
            @RequestParam("tenant") String tenant,
            @RequestParam("permission") String permission,
            @RequestHeader(AbacPolicyQueryService.TENANT_HEADER) String requestTenant
    );
}
