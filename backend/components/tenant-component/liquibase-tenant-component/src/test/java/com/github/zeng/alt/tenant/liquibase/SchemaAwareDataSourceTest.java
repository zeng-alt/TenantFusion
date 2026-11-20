package com.github.zeng.alt.tenant.liquibase;

import com.github.zeng.alt.tenant.api.TenantDialect;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 迁移连接归还连接池后，后续普通请求必须恢复默认 schema。
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class SchemaAwareDataSourceTest {
    @Test
    void restoresSchemaBeforeReturningMigrationConnectionToPool() throws Exception {
        try (final var pool = new HikariDataSource()) {
            pool.setJdbcUrl("jdbc:h2:mem:migration_schema_reset;DB_CLOSE_DELAY=-1");
            pool.setMaximumPoolSize(1);
            try (final var connection = pool.getConnection(); final var statement = connection.createStatement()) {
                statement.execute("CREATE SCHEMA TENANT_A");
            }
            final var source = new SchemaAwareDataSource(pool, new SchemaDialect(), "TENANT_A");
            try (final var connection = source.getConnection()) {
                assertThat(connection.getSchema()).isEqualTo("TENANT_A");
            }
            try (final var connection = pool.getConnection()) {
                assertThat(connection.getSchema()).isEqualTo("PUBLIC");
            }
        }
    }

    static class SchemaDialect implements TenantDialect {
        public String getName() { return "h2"; }
        public String schemaSwitchSql(String schema) { return "SET SCHEMA " + schema; }
        public String schemaResetSql() { return "SET SCHEMA PUBLIC"; }
        public String quoteIdentifier(String identifier) { return identifier; }
    }
}
