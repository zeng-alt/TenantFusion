package com.github.zeng.alt.security.data;

import io.vavr.control.Either;

/**
 * 数据权限门面；终结调用重新解析主体，不跨请求复用授权结果。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@FunctionalInterface
public interface DataPermissionTemplate {
    ResourceQuery resource(String resource);

    /**
     * 选择受保护资源的查询权限。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    @FunctionalInterface
    interface ResourceQuery {
        PermissionQuery permission(String permission);
    }

    /**
     * 计算数据范围的终结步骤。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    @FunctionalInterface
    interface PermissionQuery {
        Either<String, DataPermissionQuery> get();
    }
}
