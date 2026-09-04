package com.github.zeng.alt.tenant.liquibase;

import com.github.zeng.alt.tenant.api.TenantDialect;
import com.github.zeng.alt.tenant.api.TenantMetadata;
import com.github.zeng.alt.tenant.api.TenantMetadataProvider;
import com.github.zeng.alt.tenant.api.TenantMode;
import com.github.zeng.alt.tenant.api.TenantRouting;
import com.github.zeng.alt.tenant.api.TenantRoutingRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.DefaultResourceLoader;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 用真实 H2 验证逐租户迁移。
 * <p>
 * 这是唯一能证明该执行器有效的方式：断言每个租户的 schema 里都真的建出了表、
 * 且 {@code ${tenantName}} 参数按租户各自替换，而不是全都写成主库那个值。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
class TenantLiquibaseRunnerTest {

    private static final TenantDialect H2_DIALECT = new TenantDialect() {
        @Override
        public String getName() {
            return "h2";
        }

        @Override
        public String schemaSwitchSql(String schema) {
            return "SET SCHEMA " + requireSafeIdentifier(schema);
        }

        @Override
        public String schemaResetSql() {
            return "SET SCHEMA PUBLIC";
        }

        @Override
        public String quoteIdentifier(String identifier) {
            return "\"" + identifier + "\"";
        }
    };

    @Test
    @DisplayName("模式级租户：各自 schema 建出表，且 tenantName 参数按租户替换")
    void migratesEachSchemaTenant() throws Exception {
        String url = newDatabaseUrl();
        try (Connection keepAlive = DriverManager.getConnection(url, "sa", "")) {
            runner(url, tenant("alpha", "sch_alpha"), tenant("beta", "sch_beta")).migrateAll();

            // 两个 schema 各自都有表，且行数据里的 owner 是各自的租户标识
            assertThat(ownerIn(keepAlive, "sch_alpha")).isEqualTo("alpha");
            assertThat(ownerIn(keepAlive, "sch_beta")).isEqualTo("beta");

            // 变更记录表也落在各自 schema 内，租户之间互不干扰
            assertThat(tableExists(keepAlive, "SCH_ALPHA", "DATABASECHANGELOG")).isTrue();
            assertThat(tableExists(keepAlive, "SCH_BETA", "DATABASECHANGELOG")).isTrue();
        }
    }

    @Test
    @DisplayName("行级租户被跳过：它们共享主 schema，已由主迁移覆盖")
    void skipsRowIsolatedTenants() throws Exception {
        String url = newDatabaseUrl();
        try (Connection keepAlive = DriverManager.getConnection(url, "sa", "")) {
            TenantMetadata rowTenant = new TenantMetadata(
                    "rowly", "行级租户", TenantMode.ROW, null, null, null, true, true);
            runner(url, rowTenant).migrateAll();

            // 没有为它建任何 schema，主 schema 也没被本执行器动过
            assertThat(tableExists(keepAlive, "PUBLIC", "TENANT_DOC")).isFalse();
        }
    }

    @Test
    @DisplayName("库级租户缺少数据源定位器时给出明确错误，而不是静默迁到主库")
    void failsClearlyForDatabaseTenantWithoutLocator() throws Exception {
        String url = newDatabaseUrl();
        try (Connection keepAlive = DriverManager.getConnection(url, "sa", "")) {
            TenantMetadata dbTenant = new TenantMetadata(
                    "big", "大客户", TenantMode.DATABASE, "ds_big", null, null, null, true);
            TenantLiquibaseRunner runner = runner(url, dbTenant);

            assertThatThrownBy(runner::migrateAll)
                    .hasMessageContaining("big")
                    .hasStackTraceContaining("database-tenant-component");

            assertThat(tableExists(keepAlive, "PUBLIC", "TENANT_DOC")).isFalse();
        }
    }

    @Test
    @DisplayName("重复执行是幂等的")
    void isIdempotent() throws Exception {
        String url = newDatabaseUrl();
        try (Connection keepAlive = DriverManager.getConnection(url, "sa", "")) {
            TenantMetadata alpha = tenant("alpha", "sch_alpha");
            runner(url, alpha).migrateAll();
            runner(url, alpha).migrateAll();

            assertThat(rowCountIn(keepAlive, "sch_alpha")).isEqualTo(1);
        }
    }

    // ---------- 装配 ----------

    private static TenantMetadata tenant(String id, String schema) {
        return new TenantMetadata(id, id, TenantMode.SCHEMA, null, schema, null, false, true);
    }

    private TenantLiquibaseRunner runner(String url, TenantMetadata... tenants) {
        TenantLiquibaseProperties properties = new TenantLiquibaseProperties();
        properties.setEnabled(true);
        properties.setChangeLog("classpath:/db/tenant-test/changelog-master.yaml");

        DataSource dataSource = new SingleUrlDataSource(url);
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
        TenantDataSourceLocator locator = key -> {
            throw new com.github.zeng.alt.tenant.api.TenantRoutingException(
                    "租户数据源 [" + key + "] 无法定位：库级隔离的迁移需要引入 database-tenant-component");
        };
        return new TenantLiquibaseRunner(properties, provider, registry, dataSource,
                dialectProvider(), new DefaultResourceLoader(), locator);
    }

    private static String newDatabaseUrl() {
        return "jdbc:h2:mem:tlr_" + UUID.randomUUID().toString().replace("-", "")
                + ";DB_CLOSE_DELAY=-1";
    }

    // ---------- 断言辅助 ----------

    private static String ownerIn(Connection connection, String schema) throws SQLException {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("select owner from " + schema + ".tenant_doc")) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    private static int rowCountIn(Connection connection, String schema) throws SQLException {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("select count(*) from " + schema + ".tenant_doc")) {
            return rs.next() ? rs.getInt(1) : -1;
        }
    }

    private static boolean tableExists(Connection connection, String schema, String table)
            throws SQLException {
        try (ResultSet rs = connection.getMetaData()
                .getTables(null, schema, table, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private ObjectProvider<TenantDialect> dialectProvider() {
        return new ObjectProvider<>() {
            @Override
            public TenantDialect getObject(Object... args) {
                return H2_DIALECT;
            }

            @Override
            public TenantDialect getObject() {
                return H2_DIALECT;
            }

            @Override
            public TenantDialect getIfAvailable() {
                return H2_DIALECT;
            }

            @Override
            public TenantDialect getIfUnique() {
                return H2_DIALECT;
            }
        };
    }

    /** 每次 getConnection 都新开一条 H2 连接的极简数据源 */
    private record SingleUrlDataSource(String url) implements DataSource {
        @Override
        public Connection getConnection() throws SQLException {
            return DriverManager.getConnection(url, "sa", "");
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return DriverManager.getConnection(url, username, password);
        }

        @Override
        public java.io.PrintWriter getLogWriter() {
            return null;
        }

        @Override
        public void setLogWriter(java.io.PrintWriter out) {
        }

        @Override
        public void setLoginTimeout(int seconds) {
        }

        @Override
        public int getLoginTimeout() {
            return 0;
        }

        @Override
        public java.util.logging.Logger getParentLogger() {
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
