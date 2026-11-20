package com.github.zeng.alt.security.abac.policy;

import org.springframework.util.Assert;

/**
 * 策略集查询键，不携带用户、资源属性或认证凭据。
 *
 * @param tenant 租户标识
 * @param permission 权限 code
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public record AbacPolicyQuery(
    String tenant,
    String permission
) {
    public AbacPolicyQuery {
        Assert.hasText(tenant, "tenant must not be blank");
        Assert.hasText(permission, "permission must not be blank");
    }
}
