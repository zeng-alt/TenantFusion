package com.github.zeng.alt.tenant.api;

import java.util.Objects;

/**
 * 单个租户的路由结果——四个互相独立、可叠加的隔离旋钮。
 * <p>
 * 之所以不用单个 mode 字段表达，是因为四级隔离落在三个不同的层：
 * {@code dataSourceKey} 与 {@code schemaName} 作用在连接层，{@code tableSuffix} 作用在 SQL 文本层，
 * {@code rowIsolated} 作用在实体映射层。它们天然可以共存，例如「独立库 + 库内非默认 schema」。
 *
 * @param tenantId      租户标识，非空
 * @param dataSourceKey 数据源键；非空表示库级隔离，取 {@code alt.tenant.datasources.<key>} 下的配置
 * @param schemaName    schema 名；非空表示模式级隔离
 * @param tableSuffix   表名后缀（不含分隔符，例如 {@code t001} 会生成 {@code main_user_t001}）；非空表示表级隔离
 * @param rowIsolated   是否启用行级判别列
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
public record TenantRouting(
        String tenantId,
        String dataSourceKey,
        String schemaName,
        String tableSuffix,
        boolean rowIsolated
) {

    public TenantRouting {
        Objects.requireNonNull(tenantId, "tenantId 不能为空");
        dataSourceKey = blankToNull(dataSourceKey);
        schemaName = blankToNull(schemaName);
        tableSuffix = blankToNull(tableSuffix);
    }

    /** 完全不隔离的路由 */
    public static TenantRouting none(String tenantId) {
        return new TenantRouting(tenantId, null, null, null, false);
    }

    /** 按预设构造；schema / dataSourceKey / tableSuffix 取调用方给的名字 */
    public static TenantRouting of(String tenantId, TenantMode mode, String name) {
        if (mode == null) {
            return none(tenantId);
        }
        return switch (mode) {
            case NONE -> none(tenantId);
            case ROW -> new TenantRouting(tenantId, null, null, null, true);
            case TABLE -> new TenantRouting(tenantId, null, null, name, false);
            case SCHEMA -> new TenantRouting(tenantId, null, name, null, false);
            case DATABASE -> new TenantRouting(tenantId, name, null, null, false);
        };
    }

    /** 是否库级隔离 */
    public boolean isDatabaseIsolated() {
        return dataSourceKey != null;
    }

    /** 是否模式级隔离 */
    public boolean isSchemaIsolated() {
        return schemaName != null;
    }

    /** 是否表级隔离 */
    public boolean isTableIsolated() {
        return tableSuffix != null;
    }

    /** 是否任一档隔离生效 */
    public boolean isIsolated() {
        return isDatabaseIsolated() || isSchemaIsolated() || isTableIsolated() || rowIsolated;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
