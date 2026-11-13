package com.github.zeng.alt.admin.infrastructure.security;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 Outbox 增量迁移可重复执行、保存上下文并独立回滚。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class NamastackMigrationTest {

    @ParameterizedTest
    @ValueSource(strings = {"h2", "pg"})
    void migratesWithoutChangingExistingModulithPublications(String dialect) throws Exception {
        try (final var connection = DriverManager.getConnection("jdbc:h2:mem:namastack_" + dialect)) {
            try (final var sql = connection.createStatement()) {
                sql.execute("create table event_publication (id int primary key)");
                sql.execute("insert into event_publication values (1)");
            }
            final var database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            try (final var liquibase = new Liquibase(
                    "db/changelog/" + dialect + "/V0024-2026-11-13-namastack-outbox.sql",
                    new ClassLoaderResourceAccessor(),
                    database
            )) {
                liquibase.update(new Contexts(), new LabelExpression());
                liquibase.update(new Contexts(), new LabelExpression());
                try (final var sql = connection.createStatement()) {
                    sql.execute("""
                            insert into outbox_record
                            (id, status, record_key, record_type, payload, context, created_at,
                             failure_count, next_retry_at, partition_no, handler_id)
                            values ('1', 'NEW', 'key', 'event', '{}', '{"alt.message.tenantId":"t1"}',
                                    current_timestamp, 0, current_timestamp, 0, 'handler')
                            """);
                    try (final var rows = sql.executeQuery("select context from outbox_record")) {
                        assertThat(rows.next()).isTrue();
                        assertThat(rows.getString(1)).contains("alt.message.tenantId", "t1");
                    }
                }
                liquibase.rollback(1, "");
                try (final var sql = connection.createStatement();
                     final var rows = sql.executeQuery("select count(*) from event_publication")) {
                    rows.next();
                    assertThat(rows.getInt(1)).isEqualTo(1);
                }
            }
        }
    }
}
