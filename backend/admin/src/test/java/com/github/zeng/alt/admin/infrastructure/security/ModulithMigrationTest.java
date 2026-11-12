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
 * 验证 Modulith 2 迁移保留已有事件和完成状态，支持重复执行及回滚。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class ModulithMigrationTest {

    @Test
    void migratesExistingEventsWithoutReopeningCompletedPublications() throws Exception {
        try (final var connection = DriverManager.getConnection("jdbc:h2:mem:modulith_migration")) {
            try (final var sql = connection.createStatement()) {
                sql.execute("create table event_publication (id int primary key, completion_date timestamp, payload text)");
                sql.execute("insert into event_publication values (1, null, 'pending'), (2, current_timestamp, 'done')");
            }
            final var database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            try (final var liquibase = new Liquibase(
                    "db/changelog/h2/V0023-2026-11-12-modulith-event-lifecycle.sql",
                    new ClassLoaderResourceAccessor(), database)) {
                liquibase.update(new Contexts(), new LabelExpression());
                liquibase.update(new Contexts(), new LabelExpression());
                try (final var sql = connection.createStatement(); final var rows = sql.executeQuery(
                        "select status, completion_attempts, payload from event_publication order by id")) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getString(1)).isEqualTo("PUBLISHED");
                    assertThat(rows.getInt(2)).isZero();
                    assertThat(rows.getString(3)).isEqualTo("pending");
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getString(1)).isEqualTo("COMPLETED");
                    assertThat(rows.getString(3)).isEqualTo("done");
                    assertThat(rows.next()).isFalse();
                }
                liquibase.rollback(1, "");
                try (final var sql = connection.createStatement();
                     final var rows = sql.executeQuery("select count(*) from event_publication")) {
                    rows.next();
                    assertThat(rows.getInt(1)).isEqualTo(2);
                }
            }
        }
    }
}
