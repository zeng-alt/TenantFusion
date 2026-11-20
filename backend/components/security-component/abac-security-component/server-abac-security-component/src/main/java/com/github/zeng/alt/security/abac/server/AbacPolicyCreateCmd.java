package com.github.zeng.alt.security.abac.server;

/**
 * 创建方法策略的入参，id 由存储实现生成。
 *
 * @param tenant 租户标识
 * @param permission 方法权限码
 * @param condition 表达式文本
 * @param preAuth 是否前置校验
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public record AbacPolicyCreateCmd(
    String tenant,
    String permission,
    String condition,
    boolean preAuth
) {
}
