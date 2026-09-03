package com.github.zeng.alt.tenant.schema;

import com.github.zeng.alt.tenant.api.TenantConnectionCustomizer;
import com.github.zeng.alt.tenant.api.TenantDialect;
import com.github.zeng.alt.tenant.api.TenantRouting;
import lombok.extern.apachecommons.CommonsLog;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 模式级隔离：拿到连接后切到租户 schema，归还前复位。
 * <p>
 * 实现为 {@link TenantConnectionCustomizer} 而非独立的连接来源策略，因此天然可以和库级隔离叠加
 * ——「独立库 + 库内非默认 schema」不需要两个模块互相依赖。
 * <p>
 * 用方言 SQL 而不是 JDBC 的 {@code Connection#setSchema}：PostgreSQL 需要
 * {@code search_path} 带 {@code public} 兜底，否则留在 {@code public} 的共享表解析不到。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
@CommonsLog
public class SchemaConnectionCustomizer implements TenantConnectionCustomizer {

    private final TenantDialect dialect;

    public SchemaConnectionCustomizer(TenantDialect dialect) {
        this.dialect = dialect;
    }

    @Override
    public boolean supports(TenantRouting routing) {
        return routing.isSchemaIsolated();
    }

    @Override
    public void apply(Connection connection, TenantRouting routing) throws SQLException {
        execute(connection, dialect.schemaSwitchSql(routing.schemaName()));
    }

    /**
     * 复位失败直接抛出，由调用方销毁连接。
     * <p>带着上一个租户 schema 的连接还进池，下一个租户就会读写到别人的数据——
     * 这是模式级隔离里最危险、症状也最难排查的一类故障，绝不能吞异常。
     */
    @Override
    public void reset(Connection connection, TenantRouting routing) throws SQLException {
        execute(connection, dialect.schemaResetSql());
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
