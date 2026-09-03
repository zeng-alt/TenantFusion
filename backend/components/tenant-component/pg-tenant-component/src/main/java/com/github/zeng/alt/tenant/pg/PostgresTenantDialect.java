package com.github.zeng.alt.tenant.pg;

import com.github.zeng.alt.tenant.api.TenantDialect;

/**
 * PostgreSQL 的 schema 切换方言。
 * <p>
 * 用 {@code search_path} 而不是 JDBC 的 {@code Connection#setSchema}，是因为前者可以带
 * {@code public} 兜底：模式级隔离下共享表（如租户元数据表）仍留在 {@code public}，
 * 只设单一 schema 会让这些表解析不到。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
public class PostgresTenantDialect implements TenantDialect {

    @Override
    public String getName() {
        return "postgresql";
    }

    @Override
    public String schemaSwitchSql(String schema) {
        return "SET search_path TO " + requireSafeIdentifier(schema) + ", public";
    }

    @Override
    public String schemaResetSql() {
        return "RESET search_path";
    }

    @Override
    public String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
