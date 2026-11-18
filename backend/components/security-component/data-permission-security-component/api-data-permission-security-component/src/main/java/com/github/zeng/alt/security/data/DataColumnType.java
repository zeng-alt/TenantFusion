package com.github.zeng.alt.security.data;

/**
 * 数据权限字段的固定语义；租户字段也可使用 Hibernate 的 TenantId 声明。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public enum DataColumnType {
    TENANT,
    OWNER,
    DEPARTMENT
}
