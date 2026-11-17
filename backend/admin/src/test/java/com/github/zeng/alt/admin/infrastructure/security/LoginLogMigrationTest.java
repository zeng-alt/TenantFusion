package com.github.zeng.alt.admin.infrastructure.security;

import java.sql.DriverManager;
import java.util.UUID;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 登录元数据增量迁移保留历史 IP 和登录记录，并可重复执行。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class LoginLogMigrationTest {
    @ParameterizedTest
    @ValueSource(strings = {"h2", "pg"})
    void addsTheLocationWithoutChangingExistingLoginRows(String dialect) throws Exception {
        final String url = "jdbc:h2:mem:login_log_" + UUID.randomUUID() + ";MODE=PostgreSQL";
        try (final var connection = DriverManager.getConnection(url)) {
            try (final var sql = connection.createStatement()) {
                sql.execute("create table sys_logininfor (id bigint primary key, ip varchar(128))");
                sql.execute("insert into sys_logininfor values (1, '113.92.157.29')");
            }
            final var database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            try (final var migration = new Liquibase(
                    "db/changelog/" + dialect + "/V0025-2026-11-17-login-log-location.sql",
                    new ClassLoaderResourceAccessor(),
                    database
            )) {
                migration.update(new Contexts(), new LabelExpression());
                migration.update(new Contexts(), new LabelExpression());
                try (final var sql = connection.createStatement();
                     final var rows = sql.executeQuery("select ip, login_location from sys_logininfor where id=1")) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getString("ip")).isEqualTo("113.92.157.29");
                    assertThat(rows.getString("login_location")).isNull();
                }
                migration.rollback(1, "");
                try (final var sql = connection.createStatement();
                     final var rows = sql.executeQuery("select ip from sys_logininfor where id=1")) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getString(1)).isEqualTo("113.92.157.29");
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"h2", "pg"})
    void addsClientFieldsWithoutChangingExistingIpAndLocation(String dialect) throws Exception {
        final String url = "jdbc:h2:mem:login_client_" + UUID.randomUUID() + ";MODE=PostgreSQL";
        try (final var connection = DriverManager.getConnection(url)) {
            try (final var sql = connection.createStatement()) {
                sql.execute("create table sys_logininfor (id bigint primary key, ip varchar(128), login_location varchar(255))");
                sql.execute("insert into sys_logininfor values (1, '113.92.157.29', 'existing location')");
            }
            final var database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            try (final var migration = new Liquibase(
                    "db/changelog/" + dialect + "/V0026-2026-11-17-login-log-client.sql",
                    new ClassLoaderResourceAccessor(),
                    database
            )) {
                migration.update(new Contexts(), new LabelExpression());
                migration.update(new Contexts(), new LabelExpression());
                try (final var sql = connection.createStatement();
                     final var rows = sql.executeQuery("select ip, login_location, browser, os from sys_logininfor where id=1")) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getString("ip")).isEqualTo("113.92.157.29");
                    assertThat(rows.getString("login_location")).isEqualTo("existing location");
                    assertThat(rows.getString("browser")).isNull();
                    assertThat(rows.getString("os")).isNull();
                }
                migration.rollback(1, "");
                try (final var sql = connection.createStatement();
                     final var rows = sql.executeQuery("select login_location from sys_logininfor where id=1")) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getString(1)).isEqualTo("existing location");
                }
            }
        }
    }
}
