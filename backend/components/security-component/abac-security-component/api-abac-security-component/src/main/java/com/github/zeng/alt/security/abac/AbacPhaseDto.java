package com.github.zeng.alt.security.abac;

/**
 * 方法授权阶段及实际事务状态。
 *
 * @param preAuth 是否为前置授权
 * @param transactionActive 是否存在真实活动事务
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public record AbacPhaseDto(
    boolean preAuth,
    boolean transactionActive
) {
}
