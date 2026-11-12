package com.github.zeng.alt.admin.infrastructure.security;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 独立执行 H2 数据权限迁移及回滚，保留原用户只读授权关联。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class DataPermissionMigrationTest {
    @Test
    void shouldMigrateAndRollbackH2WithoutChangingExistingPermissionIds() throws Exception {
        try (final var connection = DriverManager.getConnection(
                "jdbc:h2:mem:data_permission_migration;DB_CLOSE_DELAY=-1")) {
            try (final var sql = connection.createStatement()) {
                sql.execute("""
                        create table main_permission (permission_id bigint primary key, resource_type varchar(64),
                        code varchar(256), name varchar(128), method varchar(16),
                        path varchar(256), tenant_by varchar(64),
                        is_enabled boolean, is_deleted boolean)
                        """);
                sql.execute("create table main_role_permission (permission_id bigint)");
                sql.execute("""
                        insert into main_permission (permission_id, method, path)
                        values (10050, 'GET', '/v1/user/{id:old}')
                        """);
            }
            final var database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            try (final var liquibase = new Liquibase(
                    "db/changelog/h2/V0022-2026-11-12-data-permission.sql",
                    new ClassLoaderResourceAccessor(), database)) {
                liquibase.setChangeLogParameter("tenantName", "master");
                liquibase.update(new Contexts(), new LabelExpression());
                try (final var sql = connection.createStatement(); final var rows = sql.executeQuery(
                        "select count(*) from main_permission "
                                + "where permission_id in (10050, 20261112001, 20261112002)")) {
                    rows.next();
                    assertThat(rows.getInt(1)).isEqualTo(3);
                }
                liquibase.rollback(1, "");
                try (final var sql = connection.createStatement();
                     final var rows = sql.executeQuery("select count(*) from main_permission")) {
                    rows.next();
                    assertThat(rows.getInt(1)).isEqualTo(1);
                }
            }
        }
    }
}
