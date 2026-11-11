package com.github.zeng.alt.security.abac;

import io.vavr.control.Option;

/**
 * 租户内单条许可条件。permission 为所属权限标识；resourceId 为空表示整个权限范围，
 * 否则只适用于对应业务对象。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public record AbacPolicy(
        String id,
        String tenant,
        String permission,
        String condition,
        boolean preAuth,
        Option<String> resourceId
) {
}
