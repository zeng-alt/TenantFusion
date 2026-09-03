package com.github.zeng.alt.tenant.h2;

import com.github.zeng.alt.tenant.api.TenantDialect;

/**
 * H2 的 schema 切换方言。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
public class H2TenantDialect implements TenantDialect {

    @Override
    public String getName() {
        return "h2";
    }

    @Override
    public String schemaSwitchSql(String schema) {
        return "SET SCHEMA " + requireSafeIdentifier(schema);
    }

    /** H2 的默认 schema 固定为 PUBLIC */
    @Override
    public String schemaResetSql() {
        return "SET SCHEMA PUBLIC";
    }

    @Override
    public String quoteIdentifier(String identifier) {
        // H2 用双引号，内部双引号需转义为两个
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
