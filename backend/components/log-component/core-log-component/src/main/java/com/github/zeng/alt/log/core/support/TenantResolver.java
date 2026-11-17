package com.github.zeng.alt.log.core.support;

import io.vavr.control.Option;

/**
 * 操作日志的租户归属扩展点。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@FunctionalInterface
public interface TenantResolver {
    /** 返回日志所属的租户；无租户上下文时为空。 */
    Option<String> currentTenant();
}
