package com.github.zeng.alt.security.abac.policy;

import org.springframework.util.Assert;

import java.util.List;

/**
 * 一个租户、权限范围内的不可变策略快照。
 *
 * @param tenant 租户标识
 * @param permission 权限 code
 * @param revision 策略集内容指纹，不用于比较通知先后顺序
 * @param policies 全部适用候选策略，空集合表示查询成功但无规则
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public record AbacPolicySnapshotDto(
    String tenant,
    String permission,
    String revision,
    List<AbacPolicyDto> policies
) {
    public AbacPolicySnapshotDto {
        Assert.hasText(tenant, "tenant must not be blank");
        Assert.hasText(permission, "permission must not be blank");
        Assert.hasText(revision, "revision must not be blank");
        policies = List.copyOf(policies);
    }
}
