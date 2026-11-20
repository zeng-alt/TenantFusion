package com.github.zeng.alt.security.abac.policy;

import com.github.zeng.alt.security.abac.AbacPolicy;

/**
 * 跨进程传输的纯文本策略，不序列化表达式执行对象。
 *
 * @param id 策略标识
 * @param tenant 租户标识
 * @param permission 方法权限 code
 * @param condition 表达式文本
 * @param preAuth 是否前置校验
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public record AbacPolicyDto(
    String id,
    String tenant,
    String permission,
    String condition,
    boolean preAuth
) {
    public static AbacPolicyDto from(AbacPolicy policy) {
        return new AbacPolicyDto(policy.id(), policy.tenant(), policy.permission(), policy.condition(), policy.preAuth());
    }

    public AbacPolicy toPolicy() {
        return new AbacPolicy(id, tenant, permission, condition, preAuth);
    }
}
