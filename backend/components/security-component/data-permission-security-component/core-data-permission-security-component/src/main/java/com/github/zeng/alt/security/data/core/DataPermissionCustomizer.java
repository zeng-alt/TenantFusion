package com.github.zeng.alt.security.data.core;




/**
 * 宿主对数据权限配置的有序贡献。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@FunctionalInterface
public interface DataPermissionCustomizer {
    void customize(DataPermissionProperties properties);
}
