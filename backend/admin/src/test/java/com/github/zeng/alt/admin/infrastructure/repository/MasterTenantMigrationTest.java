package com.github.zeng.alt.admin.infrastructure.repository;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证默认主租户从跨租户模式升级为行级隔离。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class MasterTenantMigrationTest {
    @ParameterizedTest
    @ValueSource(strings = {"h2", "pg"})
    void upgradesOnlySharedMasterTenant(String dialect) throws Exception {
        try (final var connection = DriverManager.getConnection("jdbc:h2:mem:master_migration_" + dialect);
                final var sql = connection.createStatement()) {
            sql.execute("""
                    CREATE TABLE main_tenant (tenant_id VARCHAR(64), isolation_mode VARCHAR(32),
                    is_row_isolated BOOLEAN, datasource_key VARCHAR(64), schema_name VARCHAR(64), table_suffix VARCHAR(64))
                    """);
            sql.execute("""
                    INSERT INTO main_tenant VALUES ('master', 'NONE', FALSE, NULL, NULL, NULL),
                    ('t001', 'ROW', TRUE, NULL, NULL, NULL)
                    """);
            ScriptUtils.executeSqlScript(connection, new ClassPathResource(
                    "db/changelog/tenant/" + dialect + "/V0004-2026-11-20-isolate-master-tenant.sql"));
            try (final var rows = sql.executeQuery("SELECT isolation_mode, is_row_isolated FROM main_tenant ORDER BY tenant_id")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("ROW");
                assertThat(rows.getBoolean(2)).isTrue();
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("ROW");
                assertThat(rows.getBoolean(2)).isTrue();
            }
        }
    }
}
