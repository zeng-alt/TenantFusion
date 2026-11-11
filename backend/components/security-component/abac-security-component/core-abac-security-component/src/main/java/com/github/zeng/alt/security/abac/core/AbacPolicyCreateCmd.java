package com.github.zeng.alt.security.abac.core;

import io.vavr.control.Option;

/**
 * 创建策略的入参，id 由存储实现生成。resourceId 为空表示整个权限范围。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public record AbacPolicyCreateCmd(
        String tenant,
        String permission,
        String condition,
        boolean preAuth,
        Option<String> resourceId
) {
}