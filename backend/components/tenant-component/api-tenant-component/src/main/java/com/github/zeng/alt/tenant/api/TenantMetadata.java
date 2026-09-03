package com.github.zeng.alt.tenant.api;

/**
 * 租户元数据——路由的信息源，通常来自 {@code main_tenant} 表。
 * <p>
 * 注意 {@code dataSourceKey} 只是一个键，真正的 url / 账号 / 口令放在
 * {@code alt.tenant.datasources.<key>} 配置下并由环境变量注入，不落库。
 *
 * @param tenantId      租户标识
 * @param tenantName    租户名称
 * @param mode          该租户的隔离预设；为 null 时由全局配置决定
 * @param dataSourceKey 数据源键
 * @param schemaName    schema 名
 * @param tableSuffix   表名后缀
 * @param rowIsolated   是否启用行级判别列；为 null 时由全局配置决定
 * @param enabled       是否启用
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
public record TenantMetadata(
        String tenantId,
        String tenantName,
        TenantMode mode,
        String dataSourceKey,
        String schemaName,
        String tableSuffix,
        Boolean rowIsolated,
        boolean enabled
) {
}
