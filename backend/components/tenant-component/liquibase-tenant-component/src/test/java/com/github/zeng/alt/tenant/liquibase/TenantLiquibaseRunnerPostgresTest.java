package com.github.zeng.alt.tenant.liquibase;

import com.github.zeng.alt.tenant.api.TenantDialect;
import com.github.zeng.alt.tenant.api.TenantMetadata;
import com.github.zeng.alt.tenant.api.TenantMetadataProvider;
import com.github.zeng.alt.tenant.api.TenantMode;
import com.github.zeng.alt.tenant.api.TenantRouting;
import com.github.zeng.alt.tenant.api.TenantRoutingRegistry;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.DefaultResourceLoader;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 在真实 PostgreSQL 上验证逐租户迁移。
 * <p>
 * 只在 H2 上验证不够：两者的 schema 语义并不相同——H2 用 {@code SET SCHEMA}、
 * 未加引号的标识符折叠成大写；PostgreSQL 用 {@code search_path}、折叠成小写。
 * 模式级隔离恰好完全依赖这块差异。
 * <p>
 * 连不上 PostgreSQL 时整体跳过，不影响没装数据库的环境跑测试。连接参数可用
 * {@code -Dtenant.test.pg.url} / {@code .user} / {@code .password} 覆盖。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
class TenantLiquibaseRunnerPostgresTest {

    private static final String URL =
            System.getProperty("tenant.test.pg.url", "jdbc:postgresql://127.0.0.1:5432/admin_db");
    private static final String USER = System.getProperty("tenant.test.pg.user", "admin_app");
    private static final String PASSWORD =
            System.getProperty("tenant.test.pg.password", "local_dev_only");

    /** PostgreSQL 方言，内联以免测试依赖 pg-tenant-component 模块 */
    private static final TenantDialect PG_DIALECT = new TenantDialect() {
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
            return "\"" + identifier + "\"";
        }
    };

    @Test
    @DisplayName("PostgreSQL 模式级租户：各自 schema 建表，tenantName 按租户替换")
    void migratesEachSchemaTenantOnPostgres() throws Exception {
        Assumptions.assumeTrue(reachable(), "PostgreSQL 不可用，跳过");

        String alpha = "tlr_alpha";
        String beta = "tlr_beta";
        dropSchemas(alpha, beta);
        try {
            runner(tenant("alpha", alpha), tenant("beta", beta)).migrateAll();

            assertThat(ownerIn(alpha)).isEqualTo("alpha");
            assertThat(ownerIn(beta)).isEqualTo("beta");
            // 变更记录表也各自独立，两个租户的迁移进度互不影响
            assertThat(tableExists(alpha, "databasechangelog")).isTrue();
            assertThat(tableExists(beta, "databasechangelog")).isTrue();
            // 主 schema 没被污染
            assertThat(tableExists("public", "tenant_doc")).isFalse();
        } finally {
            dropSchemas(alpha, beta);
        }
    }

    @Test
    @DisplayName("PostgreSQL 上重复执行是幂等的")
    void isIdempotentOnPostgres() throws Exception {
        Assumptions.assumeTrue(reachable(), "PostgreSQL 不可用，跳过");

        String schema = "tlr_idem";
        dropSchemas(schema);
        try {
            runner(tenant("idem", schema)).migrateAll();
            runner(tenant("idem", schema)).migrateAll();

            assertThat(rowCountIn(schema)).isEqualTo(1);
        } finally {
            dropSchemas(schema);
        }
    }

    // ---------- 装配 ----------

    private static TenantMetadata tenant(String id, String schema) {
        return new TenantMetadata(id, id, TenantMode.SCHEMA, null, schema, null, false, true);
    }

    private TenantLiquibaseRunner runner(TenantMetadata... tenants) {
        TenantLiquibaseProperties properties = new TenantLiquibaseProperties();
        properties.setEnabled(true);
        properties.setChangeLog("classpath:/db/tenant-test/changelog-master.yaml");

        TenantMetadataProvider provider = new TenantMetadataProvider() {
            @Override
            public Optional<TenantMetadata> findById(String tenantId) {
                return Stream.of(tenants).filter(t -> t.tenantId().equals(tenantId)).findFirst();
            }

            @Override
            public List<TenantMetadata> findAll() {
                return List.of(tenants);
            }
        };
        TenantRoutingRegistry registry = new TenantRoutingRegistry() {
            @Override
            public TenantRouting resolve(String tenantId) {
                TenantMetadata m = provider.findById(tenantId).orElseThrow();
                return new TenantRouting(m.tenantId(), m.dataSourceKey(), m.schemaName(),
                        m.tableSuffix(), Boolean.TRUE.equals(m.rowIsolated()));
            }

            @Override
            public void evict(String tenantId) {
            }

            @Override
            public void evictAll() {
            }
        };
        return new TenantLiquibaseRunner(properties, provider, registry, new PgDataSource(),
                dialectProvider(), new DefaultResourceLoader(),
                key -> {
                    throw new UnsupportedOperationException();
                });
    }

    // ---------- 辅助 ----------

    private static boolean reachable() {
        try (Connection ignored = DriverManager.getConnection(URL, USER, PASSWORD)) {
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private static void dropSchemas(String... schemas) throws SQLException {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement statement = connection.createStatement()) {
            for (String schema : schemas) {
                statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
            }
        }
    }

    private static String ownerIn(String schema) throws SQLException {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("select owner from " + schema + ".tenant_doc")) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    private static int rowCountIn(String schema) throws SQLException {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("select count(*) from " + schema + ".tenant_doc")) {
            return rs.next() ? rs.getInt(1) : -1;
        }
    }

    private static boolean tableExists(String schema, String table) throws SQLException {
        String sql = "select 1 from information_schema.tables "
                + "where table_schema = ? and table_name = ?";
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             var ps = connection.prepareStatement(sql)) {
            ps.setString(1, schema);
            ps.setString(2, table);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private ObjectProvider<TenantDialect> dialectProvider() {
        return new ObjectProvider<>() {
            @Override
            public TenantDialect getObject(Object... args) {
                return PG_DIALECT;
            }

            @Override
            public TenantDialect getObject() {
                return PG_DIALECT;
            }

            @Override
            public TenantDialect getIfAvailable() {
                return PG_DIALECT;
            }

            @Override
            public TenantDialect getIfUnique() {
                return PG_DIALECT;
            }
        };
    }

    /** 每次都新开连接的极简数据源 */
    private static final class PgDataSource implements DataSource {
        @Override
        public Connection getConnection() throws SQLException {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return DriverManager.getConnection(URL, username, password);
        }

        @Override
        public PrintWriter getLogWriter() {
            return null;
        }

        @Override
        public void setLogWriter(PrintWriter out) {
        }

        @Override
        public void setLoginTimeout(int seconds) {
        }

        @Override
        public int getLoginTimeout() {
            return 0;
        }

        @Override
        public Logger getParentLogger() {
            return null;
        }

        @Override
        public <T> T unwrap(Class<T> iface) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return false;
        }
    }
}
