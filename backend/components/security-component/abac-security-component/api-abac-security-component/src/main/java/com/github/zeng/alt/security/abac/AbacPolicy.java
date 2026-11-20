package com.github.zeng.alt.security.abac;

/**
 * 租户内单条方法授权条件。
 *
 * @param id 策略标识
 * @param tenant 租户标识
 * @param permission 所属方法权限
 * @param condition 表达式文本
 * @param preAuth 是否前置校验
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public record AbacPolicy(
    String id,
    String tenant,
    String permission,
    String condition,
    boolean preAuth
) {
}
